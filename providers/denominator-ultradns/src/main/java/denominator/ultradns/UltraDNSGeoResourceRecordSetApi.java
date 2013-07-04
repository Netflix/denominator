package denominator.ultradns;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.concat;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.profile.Geo.asGeo;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;

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

final class UltraDNSGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
    private static final Filter<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");
    private static final int DEFAULT_TTL = 300;

    private final Collection<String> supportedTypes;
    private final Map<String, Collection<String>> regions;
    private final UltraDNS api;
    private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;
    private final String zoneName;

    UltraDNSGeoResourceRecordSetApi(Collection<String> supportedTypes, Map<String, Collection<String>> regions, UltraDNS api,
            GroupGeoRecordByNameTypeIterator.Factory iteratorFactory, String zoneName) {
        this.supportedTypes = supportedTypes;
        this.regions = regions;
        this.api = api;
        this.iteratorFactory = iteratorFactory;
        this.zoneName = zoneName;
    }

    @Override
    public Map<String, Collection<String>> supportedRegions() {
        return regions;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        List<Iterable<ResourceRecordSet<?>>> eachPool = new ArrayList<Iterable<ResourceRecordSet<?>>>();
        for (final String poolName : api.directionalPoolNameToIdsInZone(zoneName).keySet()) {
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
            return Collections.<ResourceRecordSet<?>> emptyList().iterator();
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
    public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        checkNotNull(name, "description");
        checkNotNull(type, "type");
        checkNotNull(qualifier, "qualifier");
        if (!supportedTypes.contains(type)) {
            return null;
        }
        Iterator<DirectionalRecord> records = recordsByNameTypeAndQualifier(name, type, qualifier);
        return nextOrNull(iteratorFactory.create(records));
    }

    private Iterator<DirectionalRecord> recordsByNameTypeAndQualifier(String name, String type, String qualifier) {
        if ("CNAME".equals(type)) {
            return filter(concat(recordsForNameTypeAndQualifier(name, "A", qualifier),
                    recordsForNameTypeAndQualifier(name, "AAAA", qualifier)), isCNAME);
        } else {
            return recordsForNameTypeAndQualifier(name, type, qualifier);
        }
    }

    private Iterator<DirectionalRecord> recordsForNameTypeAndQualifier(String name, String type, String qualifier) {
        try {
            return api.directionalRecordsInZoneAndGroupByNameAndType(zoneName, qualifier, name, dirType(type))
                    .iterator();
        } catch (UltraDNSException e) {
            // TODO: implement default fallback
            switch (e.code()) {
            case UltraDNSException.GROUP_NOT_FOUND:
            case UltraDNSException.DIRECTIONALPOOL_NOT_FOUND:
                return Collections.<DirectionalRecord> emptyList().iterator();
            }
            throw e;
        }
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
        checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);
        checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s", rrset.type(),
                supportedTypes);
        int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;
        String group = rrset.qualifier();

        Map<String, Collection<String>> regions = asGeo(rrset).regions();
        DirectionalGroup directionalGroup = new DirectionalGroup();
        directionalGroup.name = group;
        directionalGroup.regionToTerritories = regions;

        List<Map<String, Object>> recordsLeftToCreate = new ArrayList<Map<String, Object>>(rrset.rdata());
        Iterator<DirectionalRecord> iterator = recordsByNameTypeAndQualifier(rrset.name(), rrset.type(), group);
        while (iterator.hasNext()) {
            DirectionalRecord record = iterator.next();
            Map<String, Object> rdata = forTypeAndRData(record.type, record.rdata);
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                if (ttlToApply != record.ttl) {
                    record.ttl = ttlToApply;
                    api.updateRecordAndDirectionalGroup(record, directionalGroup);
                    continue;
                }
                directionalGroup = api.getDirectionalGroup(record.geoGroupId);
                if (!regions.equals(directionalGroup.regionToTerritories)) {
                    directionalGroup.regionToTerritories = regions;
                    api.updateRecordAndDirectionalGroup(record, directionalGroup);
                    continue;
                }
            } else {
                api.deleteRecord(record.id);
            }
        }

        if (!recordsLeftToCreate.isEmpty()) {
            // shotgun create
            String poolId = null;
            try {
                String type = rrset.type();
                if ("CNAME".equals(type)) {
                    type = "A";
                }
                poolId = api.createDirectionalPoolInZoneForNameAndType(zoneName, rrset.name(), type);
            } catch (UltraDNSException e) {
                if (e.code() == UltraDNSException.POOL_ALREADY_EXISTS) {
                    poolId = api.directionalPoolNameToIdsInZone(zoneName).get(rrset.name());
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
                api.createRecordAndDirectionalGroupInPool(record, directionalGroup, poolId);
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
                api.deleteDirectionalRecord(record.next().id);
            } catch (UltraDNSException e) {
                // TODO: implement default fallback
                if (e.code() != UltraDNSException.DIRECTIONALPOOL_RECORD_NOT_FOUND)
                    throw e;
            }
        }
    }

    private Iterator<ResourceRecordSet<?>> iteratorForDNameAndDirectionalType(String name, int dirType) {
        List<DirectionalRecord> list;
        try {
            list = api.directionalRecordsInZoneByNameAndType(zoneName, name, dirType);
        } catch (UltraDNSException e) {
            // TODO: implement default fallback
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
        Factory(Provider provider, @Named("geo") Lazy<Map<String, Collection<String>>> regions, UltraDNS api,
                GroupGeoRecordByNameTypeIterator.Factory iteratorFactory) {
            this.supportedTypes = provider.profileToRecordTypes().get("geo");
            this.regions = regions;
            this.api = api;
            this.iteratorFactory = iteratorFactory;
        }

        @Override
        public GeoResourceRecordSetApi create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return new UltraDNSGeoResourceRecordSetApi(supportedTypes, regions.get(), api, iteratorFactory, idOrName);
        }
    }

    private final Filter<DirectionalRecord> isCNAME = new Filter<DirectionalRecord>() {
        @Override
        public boolean apply(DirectionalRecord input) {
            return "CNAME".equals(input.type);
        }
    };
}
