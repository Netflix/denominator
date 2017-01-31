package denominator.ultradns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.concat;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.toMap;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;

final class UltraDNSGeoResourceRecordSetApi implements GeoResourceRecordSetApi {

  private static final Filter<ResourceRecordSet<?>> IS_GEO = new Filter<ResourceRecordSet<?>>() {
    @Override
    public boolean apply(ResourceRecordSet<?> in) {
      return in != null && in.geo() != null;
    }
  };
  private static final int DEFAULT_TTL = 300;

  private final Collection<String> supportedTypes;
  private final Lazy<Map<String, Collection<String>>> regions;
  private final UltraDNS api;
  private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;
  private final String zoneName;
  private final Filter<DirectionalRecord> isCNAME = new Filter<DirectionalRecord>() {
    @Override
    public boolean apply(DirectionalRecord input) {
      return "CNAME".equals(input.type);
    }
  };

  UltraDNSGeoResourceRecordSetApi(Collection<String> supportedTypes,
                                  Lazy<Map<String, Collection<String>>> regions,
                                  UltraDNS api,
                                  GroupGeoRecordByNameTypeIterator.Factory iteratorFactory,
                                  String zoneName) {
    this.supportedTypes = supportedTypes;
    this.regions = regions;
    this.api = api;
    this.iteratorFactory = iteratorFactory;
    this.zoneName = zoneName;
  }

  @Override
  public Map<String, Collection<String>> supportedRegions() {
    return regions.get();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    List<Iterable<ResourceRecordSet<?>>> eachPool = new ArrayList<Iterable<ResourceRecordSet<?>>>();
    for (final String poolName : api.getDirectionalPoolsOfZone(zoneName).keySet()) {
      eachPool.add(new Iterable<ResourceRecordSet<?>>() {
        public Iterator<ResourceRecordSet<?>> iterator() {
          return iteratorForDNameAndDirectionalType(poolName, 0);
        }
      });
    }
    return concat(eachPool);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return iteratorForDNameAndDirectionalType(checkNotNull(name, "description"), 0);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    checkNotNull(name, "description");
    checkNotNull(type, "type");
    Filter<ResourceRecordSet<?>> filter = nameAndTypeEqualTo(name, type);
    if (!supportedTypes.contains(type)) {
      return Collections.<ResourceRecordSet<?>>emptyList().iterator();
    }
    if ("CNAME".equals(type)) {
      // retain original type (this will filter out A, AAAA)
      return filter(
          concat(iteratorForDNameAndDirectionalType(name, lookup("A")),
                 iteratorForDNameAndDirectionalType(name, lookup("AAAA"))), filter);
    } else if ("A".equals(type) || "AAAA".equals(type)) {
      int dirType = "AAAA".equals(type) ? lookup("AAAA") : lookup("A");
      Iterator<ResourceRecordSet<?>> iterator = iteratorForDNameAndDirectionalType(name, dirType);
      // retain original type (this will filter out CNAMEs)
      return filter(iterator, filter);
    } else {
      return iteratorForDNameAndDirectionalType(name, dirType(type));
    }
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    checkNotNull(name, "description");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "qualifier");
    if (!supportedTypes.contains(type)) {
      return null;
    }
    Iterator<DirectionalRecord> records = recordsByNameTypeAndQualifier(name, type, qualifier);
    return nextOrNull(iteratorFactory.create(records));
  }

  private Iterator<DirectionalRecord> recordsByNameTypeAndQualifier(String name, String type,
                                                                    String qualifier) {
    if ("CNAME".equals(type)) {
      return filter(
          concat(recordsForNameTypeAndQualifier(name, "A", qualifier),
                 recordsForNameTypeAndQualifier(name, "AAAA", qualifier)), isCNAME);
    } else {
      return recordsForNameTypeAndQualifier(name, type, qualifier);
    }
  }

  private Iterator<DirectionalRecord> recordsForNameTypeAndQualifier(String name, String type,
                                                                     String qualifier) {
    try {
      return api.getDirectionalDNSRecordsForGroup(zoneName, qualifier, name, dirType(type))
          .iterator();
    } catch (UltraDNSException e) {
      switch (e.code()) {
        case UltraDNSException.GROUP_NOT_FOUND:
        case UltraDNSException.DIRECTIONALPOOL_NOT_FOUND:
          return Collections.<DirectionalRecord>emptyList().iterator();
      }
      throw e;
    }
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
    checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);
    checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s",
                  rrset.type(),
                  supportedTypes);
    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;
    String group = rrset.qualifier();

    Map<String, Collection<String>> regions = rrset.geo().regions();
    DirectionalGroup directionalGroup = new DirectionalGroup();
    directionalGroup.name = group;
    directionalGroup.regionToTerritories = regions;

    List<Map<String, Object>>
        recordsLeftToCreate =
        new ArrayList<Map<String, Object>>(rrset.records());
    Iterator<DirectionalRecord>
        iterator =
        recordsByNameTypeAndQualifier(rrset.name(), rrset.type(), group);
    while (iterator.hasNext()) {
      DirectionalRecord record = iterator.next();
      Map<String, Object> rdata = toMap(record.type, record.rdata);
      if (recordsLeftToCreate.contains(rdata)) {
        recordsLeftToCreate.remove(rdata);
        boolean shouldUpdate = false;
        if (ttlToApply != record.ttl) {
          record.ttl = ttlToApply;
          shouldUpdate = true;
        } else {
          directionalGroup = api.getDirectionalDNSGroupDetails(record.geoGroupId);
          if (!regions.equals(directionalGroup.regionToTerritories)) {
            directionalGroup.regionToTerritories = regions;
            shouldUpdate = true;
          }
        }
        if (shouldUpdate) {
          try {
            api.updateDirectionalPoolRecord(record, directionalGroup);
          } catch (UltraDNSException e) {
            // lost race
            if (e.code() != UltraDNSException.RESOURCE_RECORD_ALREADY_EXISTS) {
              throw e;
            }
          }
        }
      } else {
        try {
          api.deleteResourceRecord(record.id);
        } catch (UltraDNSException e) {
          // lost race
          if (e.code() != UltraDNSException.RESOURCE_RECORD_NOT_FOUND) {
            throw e;
          }
        }
      }
    }

    if (!recordsLeftToCreate.isEmpty()) {
      // shotgun create
      String poolId;
      try {
        String type = rrset.type();
        if ("CNAME".equals(type)) {
          type = "A";
        }
        poolId = api.addDirectionalPool(zoneName, rrset.name(), type);
      } catch (UltraDNSException e) {
        // lost race
        if (e.code() == UltraDNSException.POOL_ALREADY_EXISTS) {
          poolId = api.getDirectionalPoolsOfZone(zoneName).get(rrset.name());
        } else {
          throw e;
        }
      }
      DirectionalRecord record = new DirectionalRecord();
      record.type = rrset.type();
      record.ttl = ttlToApply;

      for (Map<String, Object> rdata : recordsLeftToCreate) {
        for (Object rdatum : rdata.values()) {
          record.rdata.add(rdatum.toString());
        }
        try {
          api.addDirectionalPoolRecord(record, directionalGroup, poolId);
        } catch (UltraDNSException e) {
          // lost race
          if (e.code() != UltraDNSException.POOL_RECORD_ALREADY_EXISTS) {
            throw e;
          }
        }
      }
    }
  }

  private int dirType(String type) {
    if ("A".equals(type) || "CNAME".equals(type)) {
      return lookup("A");
    } else if ("AAAA".equals(type)) {
      return lookup("AAAA");
    } else {
      return lookup(type);
    }
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    Iterator<DirectionalRecord> record = recordsByNameTypeAndQualifier(name, type, qualifier);
    while (record.hasNext()) {
      try {
        api.deleteDirectionalPoolRecord(record.next().id);
      } catch (UltraDNSException e) {
        // lost race
        if (e.code() != UltraDNSException.DIRECTIONALPOOL_RECORD_NOT_FOUND) {
          throw e;
        }
      }
    }
  }

  private Iterator<ResourceRecordSet<?>> iteratorForDNameAndDirectionalType(String name,
                                                                            int dirType) {
    List<DirectionalRecord> list;
    try {
      list = api.getDirectionalDNSRecordsForHost(zoneName, name, dirType);
    } catch (UltraDNSException e) {
      if (e.code() == UltraDNSException.DIRECTIONALPOOL_NOT_FOUND) {
        list = Collections.emptyList();
      } else {
        throw e;
      }
    }
    return iteratorFactory.create(list.iterator());
  }

  static final class Factory implements GeoResourceRecordSetApi.Factory {

    private final Collection<String> supportedTypes;
    private final Lazy<Map<String, Collection<String>>> regions;
    private final UltraDNS api;
    private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;

    @Inject
    Factory(Provider provider, @Named("geo") Lazy<Map<String, Collection<String>>> regions,
            UltraDNS api,
            GroupGeoRecordByNameTypeIterator.Factory iteratorFactory) {
      this.supportedTypes = provider.profileToRecordTypes().get("geo");
      this.regions = regions;
      this.api = api;
      this.iteratorFactory = iteratorFactory;
    }

    @Override
    public GeoResourceRecordSetApi create(String name) {
      checkNotNull(name, "name was null");
      // Eager fetch of regions to determine if directional records are supported or not.
      try {
        regions.get();
      } catch (UltraDNSException e) {
        if (e.code() == UltraDNSException.DIRECTIONAL_NOT_ENABLED) {
          return null;
        }
        throw e;
      }
      return new UltraDNSGeoResourceRecordSetApi(supportedTypes, regions, api, iteratorFactory,
                                                 name);
    }
  }
}
