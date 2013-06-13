package denominator.ultradns;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Lists.newArrayList;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.toProfile;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;
import static denominator.ultradns.UltraDNSPredicates.isGeolocationPool;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV4;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV6;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import dagger.Lazy;
import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

public final class UltraDNSGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_GEO = profileContainsType(Geo.class);
    private static final int DEFAULT_TTL = 300;

    private final Set<String> types;
    private final Multimap<String, String> regions;
    private final DirectionalGroupApi groupApi;
    private final DirectionalPoolApi poolApi;
    private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;
    private final String zoneName;

    UltraDNSGeoResourceRecordSetApi(Set<String> types, Multimap<String, String> regions, DirectionalGroupApi groupApi,
            DirectionalPoolApi poolApi, GroupGeoRecordByNameTypeIterator.Factory iteratorFactory, String zoneName) {
        this.types = types;
        this.regions = regions;
        this.groupApi = groupApi;
        this.poolApi = poolApi;
        this.iteratorFactory = iteratorFactory;
        this.zoneName = zoneName;
    }

    @Override
    @Deprecated
    public Set<String> getSupportedTypes() {
        return supportedTypes();
    }

    @Override
    public Set<String> supportedTypes() {
        return types;
    }

    @Override
    @Deprecated
    public Multimap<String, String> getSupportedRegions() {
        return supportedRegions();
    }

    @Override
    public Multimap<String, String> supportedRegions() {
        return regions;
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
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
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return iterateByName(name);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return iteratorForDNameAndDirectionalType(checkNotNull(name, "name"), 0);
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
        return iterateByNameAndType(name, type);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
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
    @Deprecated
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group) {
        return getByNameTypeAndQualifier(name, type, group);
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
        Iterator<DirectionalPoolRecordDetail> records = recordsByNameTypeAndQualifier(name, type, qualifier);
        Iterator<ResourceRecordSet<?>> iterator = iteratorFactory.create(records);
        if (iterator.hasNext())
            return Optional.<ResourceRecordSet<?>> of(iterator.next());
        return Optional.absent();
    }

    private Iterator<DirectionalPoolRecordDetail> recordsByNameTypeAndQualifier(String name, String type,
            String qualifier) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(qualifier, "qualifier");
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
    public void put(final ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier().isPresent(), "no qualifier on: %s", rrset);
        checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);

        int ttlToApply = rrset.ttl().or(DEFAULT_TTL);
        String group = rrset.qualifier().get();

        Multimap<String, String> regions = toProfile(Geo.class).apply(rrset).regions();

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset);
        for (Iterator<DirectionalPoolRecordDetail> references = recordsByNameTypeAndQualifier(rrset.name(),
                rrset.type(), group); references.hasNext();) {
            DirectionalPoolRecordDetail reference = references.next();
            DirectionalPoolRecord record = reference.getRecord();
            Map<String, Object> rdata = forTypeAndRData(record.getType(), record.getRData());
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                boolean shouldUpdate = false;
                DirectionalGroup directionalGroup = groupApi.get(reference.getGeolocationGroup().get().getId());
                if (ttlToApply != record.getTTL()) {
                    shouldUpdate = true;
                    record = record.toBuilder().ttl(ttlToApply).build();
                }
                if (!regions.equals(directionalGroup.getRegionToTerritories())) {
                    shouldUpdate = true;
                    directionalGroup = directionalGroup.toBuilder().regionToTerritories(regions).build();
                    poolApi.updateRecordAndGroup(reference.getId(), record, directionalGroup);
                } else if (shouldUpdate) {
                    poolApi.updateRecord(reference.getId(), record);
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
            DirectionalGroup directionalGroup = DirectionalGroup.builder()//
                    .name(group)//
                    .description(group)//
                    .regionToTerritories(regions).build();

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

    @Override
    @Deprecated
    public void applyRegionsToNameTypeAndGroup(Multimap<String, String> regions, String name, String type, String group) {
        Iterator<DirectionalPoolRecordDetail> iterator = recordsByNameTypeAndQualifier(name, type, group);
        Map<DirectionalPoolRecordDetail, DirectionalGroup> updates = groupsToUpdate(iterator, regions);
        if (updates.isEmpty())
            return;
        for (Entry<DirectionalPoolRecordDetail, DirectionalGroup> update : updates.entrySet()) {
            DirectionalPoolRecordDetail detail = update.getKey();
            // TODO: ensure forceOverlapTransfer (Dodgers release of UltraDNS)
            poolApi.updateRecordAndGroup(detail.getId(), detail.getRecord(), update.getValue());
        }
    }

    private Map<DirectionalPoolRecordDetail, DirectionalGroup> groupsToUpdate(
            Iterator<DirectionalPoolRecordDetail> iterator, Multimap<String, String> regions) {
        Builder<DirectionalPoolRecordDetail, DirectionalGroup> toUpdate = ImmutableMap.builder();

        for (Iterator<DirectionalPoolRecordDetail> i = iterator; i.hasNext();) {
            DirectionalPoolRecordDetail detail = i.next();
            DirectionalGroup directionalGroup = groupApi.get(detail.getGeolocationGroup().get().getId());
            if (!regions.equals(directionalGroup.getRegionToTerritories())) {
                toUpdate.put(detail, directionalGroup.toBuilder().regionToTerritories(regions).build());
            }
        }
        return toUpdate.build();
    }

    @Override
    @Deprecated
    public void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group) {
        for (Iterator<DirectionalPoolRecordDetail> i = recordsByNameTypeAndQualifier(name, type, group); i.hasNext();) {
            DirectionalPoolRecordDetail detail = i.next();
            DirectionalPoolRecord record = detail.getRecord();
            if (record.getTTL() != ttl)
                poolApi.updateRecord(detail.getId(), record.toBuilder().ttl(ttl).build());
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
        private final Set<String> types;
        private final Lazy<Multimap<String, String>> regions;
        private final UltraDNSWSApi api;
        private final Supplier<IdAndName> account;
        private final GroupGeoRecordByNameTypeIterator.Factory iteratorFactory;

        @Inject
        Factory(@denominator.config.profile.Geo Set<String> types,
                @denominator.config.profile.Geo Lazy<Multimap<String, String>> regions, UltraDNSWSApi api,
                Supplier<IdAndName> account, GroupGeoRecordByNameTypeIterator.Factory iteratorFactory) {
            this.types = types;
            this.regions = regions;
            this.api = api;
            this.account = account;
            this.iteratorFactory = iteratorFactory;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return Optional.<GeoResourceRecordSetApi> of(new UltraDNSGeoResourceRecordSetApi(types, regions.get(), api
                    .getDirectionalGroupApiForAccount(account.get().getId()), api
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
