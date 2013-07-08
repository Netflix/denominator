package denominator.ultradns;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Lists.newArrayList;
import static denominator.ResourceTypeToValue.lookup;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.model.profile.Geo.asGeo;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import dagger.Lazy;
import denominator.Provider;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.Record;

final class UltraDNSGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");
    private static final int DEFAULT_TTL = 300;

    private final Set<String> supportedTypes;
    private final Multimap<String, String> regions;
    private final UltraDNS api;
    private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;
    private final String zoneName;

    UltraDNSGeoResourceRecordSetApi(Set<String> supportedTypes, Multimap<String, String> regions, UltraDNS api,
            GroupGeoRecordByNameTypeIterator.Factory iteratorFactory, String zoneName) {
        this.supportedTypes = supportedTypes;
        this.regions = regions;
        this.api = api;
        this.iteratorFactory = iteratorFactory;
        this.zoneName = zoneName;
    }

    @Override
    public Multimap<String, String> supportedRegions() {
        return regions;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return concat(Iterables.transform(api.directionalPoolNameToIdsInZone(zoneName).keySet(),
                new Function<String, Iterator<ResourceRecordSet<?>>>() {
                    @Override
                    public Iterator<ResourceRecordSet<?>> apply(String poolName) {
                        return iteratorForDNameAndDirectionalType(poolName, 0);
                    }
                }).iterator());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return iteratorForDNameAndDirectionalType(checkNotNull(name, "description"), 0);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(name, "description");
        checkNotNull(type, "type");
        if (!supportedTypes.contains(type)) {
            return emptyIterator();
        }
        if ("CNAME".equals(type)) {
            // retain original type (this will filter out A, AAAA)
            return filter(
                    concat(iteratorForDNameAndDirectionalType(name, lookup("A")),
                            iteratorForDNameAndDirectionalType(name, lookup("AAAA"))), typeEqualTo(type));
        } else if ("A".equals(type) || "AAAA".equals(type)) {
            int dirType = "AAAA".equals(type) ? lookup("AAAA") : lookup("A");
            Iterator<ResourceRecordSet<?>> iterator = iteratorForDNameAndDirectionalType(name, dirType);
            // retain original type (this will filter out CNAMEs)
            return filter(iterator, typeEqualTo(type));
        } else {
            return iteratorForDNameAndDirectionalType(name, dirType(type));
        }
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        checkNotNull(name, "description");
        checkNotNull(type, "type");
        checkNotNull(qualifier, "qualifier");
        if (!supportedTypes.contains(type)) {
            return Optional.absent();
        }
        List<DirectionalRecord> records = recordsByNameTypeAndQualifier(name, type, qualifier);
        Iterator<ResourceRecordSet<?>> iterator = iteratorFactory.create(records.iterator());
        if (iterator.hasNext())
            return Optional.<ResourceRecordSet<?>> of(iterator.next());
        return Optional.absent();
    }

    private List<DirectionalRecord> recordsByNameTypeAndQualifier(String name, String type, String qualifier) {
        if ("CNAME".equals(type)) {
            return FluentIterable
                    .from(Iterables.concat(recordsForNameTypeAndQualifier(name, "A", qualifier),
                            recordsForNameTypeAndQualifier(name, "AAAA", qualifier))).filter(isCNAME).toList();
        } else {
            return recordsForNameTypeAndQualifier(name, type, qualifier);
        }
    }

    private List<DirectionalRecord> recordsForNameTypeAndQualifier(String name, String type, String qualifier) {
        try {
            return api.directionalRecordsInZoneAndGroupByNameAndType(zoneName, qualifier, name, dirType(type));
        } catch (UltraDNSException e) {
            // TODO: implement default fallback
            switch (e.code()) {
            case UltraDNSException.GROUP_NOT_FOUND:
            case UltraDNSException.DIRECTIONALPOOL_NOT_FOUND:
                return ImmutableList.of();
            }
            throw e;
        }
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier().isPresent(), "no qualifier on: %s", rrset);
        checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);
        checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s", rrset.type(),
                supportedTypes);
        int ttlToApply = rrset.ttl().or(DEFAULT_TTL);
        String group = rrset.qualifier().get();

        Multimap<String, String> regions = asGeo(rrset).regions();
        DirectionalGroup directionalGroup = new DirectionalGroup();
        directionalGroup.name = group;
        directionalGroup.regionToTerritories = regions;

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset.records());
        for (DirectionalRecord record : recordsByNameTypeAndQualifier(rrset.name(), rrset.type(), group)) {
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
                record.rdata = ImmutableList.copyOf(transform(rdata.values(), toStringFunction()));
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
        for (Record record : recordsByNameTypeAndQualifier(name, type, qualifier)) {
            try {
                api.deleteDirectionalRecord(record.id);
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
                list = ImmutableList.of();
            } else {
                throw e;
            }
        }
        return iteratorFactory.create(byTypeAndGeoGroup.immutableSortedCopy(list).iterator());
    }

    private static final Ordering<DirectionalRecord> byTypeAndGeoGroup = new Ordering<DirectionalRecord>() {

        @Override
        public int compare(DirectionalRecord left, DirectionalRecord right) {
            checkState(left.geoGroupName != null, "expected record to be in a geolocation qualifier: %s", left);
            checkState(right.geoGroupName != null, "expected record to be in a geolocation qualifier: %s", right);
            return ComparisonChain.start().compare(left.type, right.type)
                    .compare(left.geoGroupName, right.geoGroupName).result();
        }
    };

    static final class Factory implements GeoResourceRecordSetApi.Factory {
        private final Set<String> supportedTypes;
        private final Lazy<Multimap<String, String>> regions;
        private final UltraDNS api;
        private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;

        @Inject
        Factory(Provider provider, @Named("geo") Lazy<Multimap<String, String>> regions, UltraDNS api,
                GroupGeoRecordByNameTypeIterator.Factory iteratorFactory) {
            this.supportedTypes = provider.profileToRecordTypes().get("geo");
            this.regions = regions;
            this.api = api;
            this.iteratorFactory = iteratorFactory;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return Optional.<GeoResourceRecordSetApi> of(new UltraDNSGeoResourceRecordSetApi(supportedTypes, regions
                    .get(), api, iteratorFactory, idOrName));
        }
    }

    private final Predicate<DirectionalRecord> isCNAME = new Predicate<DirectionalRecord>() {
        @Override
        public boolean apply(DirectionalRecord input) {
            return "CNAME".equals(input.type);
        }
    };
}
