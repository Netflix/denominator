package denominator.ultradns;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Lists.newArrayList;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.model.profile.Geo.asGeo;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;
import static denominator.ultradns.UltraDNSPredicates.isGeolocationPool;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV4;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV6;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSExceptions.ResourceAlreadyExistsException;
import org.jclouds.ultradns.ws.domain.DirectionalGroup;
import org.jclouds.ultradns.ws.domain.DirectionalGroupCoordinates;
import org.jclouds.ultradns.ws.domain.DirectionalPool;
import org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType;
import org.jclouds.ultradns.ws.domain.DirectionalPoolRecord;
import org.jclouds.ultradns.ws.domain.DirectionalPoolRecordDetail;
import org.jclouds.ultradns.ws.domain.IdAndName;
import org.jclouds.ultradns.ws.features.DirectionalGroupApi;
import org.jclouds.ultradns.ws.features.DirectionalPoolApi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import dagger.Lazy;
import denominator.Provider;
import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

public final class UltraDNSGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");
    private static final int DEFAULT_TTL = 300;

    private final Set<String> supportedTypes;
    private final Multimap<String, String> regions;
    private final DirectionalGroupApi groupApi;
    private final DirectionalPoolApi poolApi;
    private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;
    private final String zoneName;

    UltraDNSGeoResourceRecordSetApi(Set<String> supportedTypes, Multimap<String, String> regions, DirectionalGroupApi groupApi,
            DirectionalPoolApi poolApi, GroupGeoRecordByNameTypeIterator.Factory iteratorFactory, String zoneName) {
        this.supportedTypes = supportedTypes;
        this.regions = regions;
        this.groupApi = groupApi;
        this.poolApi = poolApi;
        this.iteratorFactory = iteratorFactory;
        this.zoneName = zoneName;
    }

    @Override
    public Multimap<String, String> supportedRegions() {
        return regions;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return concat(poolApi.list().filter(isGeolocationPool())
                .transform(new Function<DirectionalPool, Iterator<ResourceRecordSet<?>>>() {
                    @Override
                    public Iterator<ResourceRecordSet<?>> apply(DirectionalPool pool) {
                        return iteratorForDNameAndDirectionalType(pool.getDName(), 0);
                    }
                }).iterator());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return iteratorForDNameAndDirectionalType(checkNotNull(name, "name"), 0);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        if (!supportedTypes.contains(type)){
            return emptyIterator();
        }
        if ("CNAME".equals(type)) {
            // retain original type (this will filter out A, AAAA)
            return filter(
                    concat(iteratorForDNameAndDirectionalType(name, IPV4.getCode()),
                            iteratorForDNameAndDirectionalType(name, IPV6.getCode())), typeEqualTo(type));
        } else if ("A".equals(type) || "AAAA".equals(type)) {
            RecordType dirType = "AAAA".equals(type) ? IPV6 : IPV4;
            Iterator<ResourceRecordSet<?>> iterator = iteratorForDNameAndDirectionalType(name, dirType.getCode());
            // retain original type (this will filter out CNAMEs)
            return filter(iterator, typeEqualTo(type));
        } else {
            return iteratorForDNameAndDirectionalType(name, RecordType.valueOf(type).getCode());
        }
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(qualifier, "qualifier");
        if (!supportedTypes.contains(type)){
            return Optional.absent();
        }
        Iterator<DirectionalPoolRecordDetail> records = recordsByNameTypeAndQualifier(name, type, qualifier);
        Iterator<ResourceRecordSet<?>> iterator = iteratorFactory.create(records);
        if (iterator.hasNext())
            return Optional.<ResourceRecordSet<?>> of(iterator.next());
        return Optional.absent();
    }

    private Iterator<DirectionalPoolRecordDetail> recordsByNameTypeAndQualifier(String name, String type,
            String qualifier) {
        Iterator<DirectionalPoolRecordDetail> records;
        if ("CNAME".equals(type)) {
            records = filter(
                    concat(recordsForNameTypeAndQualifier(name, "A", qualifier),
                            recordsForNameTypeAndQualifier(name, "AAAA", qualifier)), isCNAME);
        } else {
            records = recordsForNameTypeAndQualifier(name, type, qualifier);
        }
        return records;
    }

    private Iterator<DirectionalPoolRecordDetail> recordsForNameTypeAndQualifier(String name, String type,
            String qualifier) {
        int typeValue = checkNotNull(new ResourceTypeToValue().get(type), "typeValue for %s", type);
        DirectionalGroupCoordinates coord = DirectionalGroupCoordinates.builder().zoneName(zoneName).recordName(name)
                .recordType(typeValue).groupName(qualifier).build();
        return groupApi.listRecordsByGroupCoordinates(coord).iterator();
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier().isPresent(), "no qualifier on: %s", rrset);
        checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);
        checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s", rrset.type(), supportedTypes);
        int ttlToApply = rrset.ttl().or(DEFAULT_TTL);
        String group = rrset.qualifier().get();

        Multimap<String, String> regions = asGeo(rrset).regions();
        DirectionalGroup directionalGroup = DirectionalGroup.builder()//
                                                            .name(group)//
                                                            .description(group)//
                                                            .regionToTerritories(regions).build();

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset.rdata());
        for (Iterator<DirectionalPoolRecordDetail> references = recordsByNameTypeAndQualifier(rrset.name(),
                rrset.type(), group); references.hasNext();) {
            DirectionalPoolRecordDetail reference = references.next();
            DirectionalPoolRecord record = reference.getRecord();
            Map<String, Object> rdata = forTypeAndRData(record.getType(), record.getRData());
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                if (ttlToApply != record.getTTL()) {
                    record = record.toBuilder().ttl(ttlToApply).build();
                    poolApi.updateRecordAndGroup(reference.getId(), record, directionalGroup);
                    continue;
                }
                directionalGroup = groupApi.get(reference.getGeolocationGroup().get().getId());
                if (!regions.equals(directionalGroup.getRegionToTerritories())) {
                    directionalGroup = directionalGroup.toBuilder().regionToTerritories(regions).build();
                    poolApi.updateRecordAndGroup(reference.getId(), record, directionalGroup);
                    continue;
                }
            } else {
                poolApi.deleteRecord(reference.getId());
            }
        }

        if (!recordsLeftToCreate.isEmpty()) {
            // shotgun create
            String poolId = null;
            try {
                poolId = poolApi.createForDNameAndType(rrset.name(), rrset.name(), dirType(rrset.type()).getCode());
            } catch (ResourceAlreadyExistsException e) {
                poolId = poolIdForDName(rrset.name());
            }

            DirectionalPoolRecord.Builder builder = DirectionalPoolRecord.drBuilder()//
                    .type(rrset.type())//
                    .ttl(ttlToApply);

            for (Map<String, Object> rdata : recordsLeftToCreate) {
                DirectionalPoolRecord record = builder.rdata(rdata.values()).build();
                poolApi.addRecordIntoNewGroup(poolId, record, directionalGroup);
            }
        }
    }

    private String poolIdForDName(String dname) {
        String poolId = null;
        for (DirectionalPool in : poolApi.list()) {
            if (dname.equals(in.getDName())) {
                poolId = in.getId();
                break;
            }
        }
        return poolId;
    }

    private RecordType dirType(String type) {
        final RecordType dirType;
        if ("A".equals(type) || "CNAME".equals(type)) {
            dirType = IPV4;
        } else if ("AAAA".equals(type)) {
            dirType = IPV6;
        } else {
            dirType = RecordType.valueOf(type);
        }
        return dirType;
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        Iterator<DirectionalPoolRecordDetail> toDelete = recordsByNameTypeAndQualifier(name, type, qualifier);
        while (toDelete.hasNext()) {
            poolApi.deleteRecord(toDelete.next().getId());
        }
    }

    private Iterator<ResourceRecordSet<?>> iteratorForDNameAndDirectionalType(String name, int dirType) {
        return iteratorFactory.create(poolApi.listRecordsByDNameAndType(name, dirType).toSortedList(byTypeAndGeoGroup)
                .iterator());
    }

    static Optional<IdAndName> qualifier(DirectionalPoolRecordDetail in) {
        return in.getGeolocationGroup().or(in.getGroup());
    }

    private static final Ordering<DirectionalPoolRecordDetail> byTypeAndGeoGroup = new Ordering<DirectionalPoolRecordDetail>() {

        @Override
        public int compare(DirectionalPoolRecordDetail left, DirectionalPoolRecordDetail right) {
            checkState(qualifier(left).isPresent(), "expected record to be in a geolocation qualifier: %s", left);
            checkState(qualifier(right).isPresent(), "expected record to be in a geolocation qualifier: %s", right);
            return ComparisonChain.start().compare(left.getRecord().getType(), right.getRecord().getType())
                    .compare(qualifier(left).get().getName(), qualifier(right).get().getName()).result();
        }
    };

    static final class Factory implements GeoResourceRecordSetApi.Factory {
        private final Set<String> supportedTypes;
        private final Lazy<Multimap<String, String>> regions;
        private final UltraDNSWSApi api;
        private final Supplier<IdAndName> account;
        private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;

        @Inject
        Factory(Provider provider, @Named("geo") Lazy<Multimap<String, String>> regions, UltraDNSWSApi api,
                Supplier<IdAndName> account, GroupGeoRecordByNameTypeIterator.Factory iteratorFactory) {
            this.supportedTypes = provider.profileToRecordTypes().get("geo");
            this.regions = regions;
            this.api = api;
            this.account = account;
            this.iteratorFactory = iteratorFactory;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return Optional.<GeoResourceRecordSetApi> of(new UltraDNSGeoResourceRecordSetApi(supportedTypes, regions
                    .get(), api.getDirectionalGroupApiForAccount(account.get().getId()), api
                    .getDirectionalPoolApiForZone(idOrName), iteratorFactory, idOrName));
        }
    }

    private final Predicate<DirectionalPoolRecordDetail> isCNAME = new Predicate<DirectionalPoolRecordDetail>() {
        @Override
        public boolean apply(DirectionalPoolRecordDetail input) {
            return "CNAME".equals(input.getRecord().getType());
        }
    };
}
