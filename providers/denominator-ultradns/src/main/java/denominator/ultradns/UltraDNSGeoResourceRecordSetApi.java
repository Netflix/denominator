package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.ultradns.UltraDNSPredicates.isGeolocationPool;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV4;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV6;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
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
import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

public final class UltraDNSGeoResourceRecordSetApi implements GeoResourceRecordSetApi {

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
    public Set<String> getSupportedTypes() {
        return types;
    }

    @Override
    public Multimap<String, String> getSupportedRegions() {
        return regions;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return concat(poolApi.list().filter(isGeolocationPool())
                .transform(new Function<DirectionalPool, Iterator<ResourceRecordSet<?>>>() {
                    @Override
                    public Iterator<ResourceRecordSet<?>> apply(DirectionalPool pool) {
                        return allByDName(pool.getDName());
                    }
                }).iterator());
    }

    private Iterator<ResourceRecordSet<?>> allByDName(final String dname) {
        // TODO: remove when type "0" can be specified
        return concat(transform(EnumSet.allOf(RecordType.class).iterator(),
                new Function<RecordType, Iterator<ResourceRecordSet<?>>>() {
                    @Override
                    public Iterator<ResourceRecordSet<?>> apply(RecordType input) {
                        return iteratorForDNameAndDirectionalType(dname, input);
                    }
                }));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return allByDName(checkNotNull(name, "name"));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, final String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        if ("CNAME".equals(type)) {
            // retain original type (this will filter out A, AAAA)
            return filter(
                    concat(iteratorForDNameAndDirectionalType(name, IPV4), 
                           iteratorForDNameAndDirectionalType(name, IPV6)),
                    typeEqualTo(type));
        } else if ("A".equals(type) || "AAAA".equals(type)) {
            RecordType dirType = "AAAA".equals(type) ? IPV6 : IPV4;
            Iterator<ResourceRecordSet<?>> iterator = iteratorForDNameAndDirectionalType(name, dirType);
            // retain original type (this will filter out CNAMEs)
            return filter(iterator, typeEqualTo(type));
        } else {
            return iteratorForDNameAndDirectionalType(name, RecordType.valueOf(type));
        }
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type,
            String group) {
        Iterator<DirectionalPoolRecordDetail> records = recordsByNameTypeAndGroupName(name, type, group);
        Iterator<ResourceRecordSet<?>> iterator = iteratorFactory.create(records);
        if (iterator.hasNext())
            return Optional.<ResourceRecordSet<?>> of(iterator.next());
        return Optional.absent();
    }

    private Iterator<DirectionalPoolRecordDetail> recordsByNameTypeAndGroupName(String name, String type,
            String group) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(group, "group");
        Iterator<DirectionalPoolRecordDetail> records;
        if ("CNAME".equals(type)) {
            records = filter(
                        concat(recordsForNameTypeAndGroup(name, "A", group),
                               recordsForNameTypeAndGroup(name, "AAAA", group)), isCNAME);
        } else {
            records = recordsForNameTypeAndGroup(name, type, group);
        }
        return records;
    }

    private Iterator<DirectionalPoolRecordDetail> recordsForNameTypeAndGroup(String name, String type, String group) {
        int typeValue = checkNotNull(new ResourceTypeToValue().get(type), "typeValue for %s", type);
        DirectionalGroupCoordinates coord = DirectionalGroupCoordinates.builder()
                                                                       .zoneName(zoneName)
                                                                       .recordName(name)
                                                                       .recordType(typeValue)
                                                                       .groupName(group).build();
        return groupApi.listRecordsByGroupCoordinates(coord).iterator();
    }

    @Override
    public void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group) {
        for (Iterator<DirectionalPoolRecordDetail> i = recordsByNameTypeAndGroupName(name, type, group); i.hasNext();) {
            DirectionalPoolRecordDetail detail = i.next();
            DirectionalPoolRecord record = detail.getRecord();
            if (record.getTTL() != ttl)
                poolApi.updateRecord(detail.getId(), record.toBuilder().ttl(ttl).build());
        }
    }

    private Iterator<ResourceRecordSet<?>> iteratorForDNameAndDirectionalType(String name, RecordType dirType) {
        return iteratorFactory.create(poolApi.listRecordsByDNameAndType(name, dirType.getCode())
                                             .toSortedList(byTypeAndGeoGroup).iterator());
    }

    static Optional<IdAndName> group(DirectionalPoolRecordDetail in) {
        return in.getGeolocationGroup().or(in.getGroup());
    }

    private static final Ordering<DirectionalPoolRecordDetail> byTypeAndGeoGroup = new Ordering<DirectionalPoolRecordDetail>() {

        @Override
        public int compare(DirectionalPoolRecordDetail left, DirectionalPoolRecordDetail right) {
            checkState(group(left).isPresent(), "expected record to be in a geolocation group: %s", left);
            checkState(group(right).isPresent(), "expected record to be in a geolocation group: %s", right);
            return ComparisonChain.start()
                                  .compare(left.getRecord().getType(), right.getRecord().getType())
                                  .compare(group(left).get().getName(), group(right).get().getName()).result();
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
        public Optional<GeoResourceRecordSetApi> create(String zoneName) {
            checkNotNull(zoneName, "zoneName was null");
            return Optional.<GeoResourceRecordSetApi> of(
                    new UltraDNSGeoResourceRecordSetApi(types, regions.get(), 
                            api.getDirectionalGroupApiForAccount(account.get().getId()),
                            api.getDirectionalPoolApiForZone(zoneName), iteratorFactory, zoneName));
        }
    }

    private final Predicate<DirectionalPoolRecordDetail> isCNAME = new Predicate<DirectionalPoolRecordDetail>() {
        @Override
        public boolean apply(DirectionalPoolRecordDetail input) {
            return "CNAME".equals(input.getRecord().getType());
        }
    };
}
