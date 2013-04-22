package denominator.ultradns;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Lists.newArrayList;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.ultradns.UltraDNSFunctions.toRdataMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail;
import org.jclouds.ultradns.ws.features.ResourceRecordApi;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;

import denominator.ResourceRecordSetApi;
import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;

public final class UltraDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final UltraDNSWSApi api;

        @Inject
        Factory(UltraDNSWSApi api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(final String zoneName) {
            return new UltraDNSResourceRecordSetApi(api.getResourceRecordApiForZone(zoneName),
                    new UltraDNSRoundRobinPoolApi(api.getRoundRobinPoolApiForZone(zoneName)));
        }
    }

    private final ResourceRecordApi api;
    private final UltraDNSRoundRobinPoolApi roundRobinPoolApi;

    UltraDNSResourceRecordSetApi(ResourceRecordApi api, UltraDNSRoundRobinPoolApi roundRobinPoolApi) {
        this.api = api;
        this.roundRobinPoolApi = roundRobinPoolApi;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        // this will list all normal or RR pool records.
        Iterator<ResourceRecordDetail> orderedRecords = api.list().toSortedList(byNameTypeAndCreateDate).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        checkNotNull(name, "name");
        // TODO: temporary until listByNameAndType() works with NS records where
        // name = zoneName
        return Iterators.filter(list(), nameEqualTo(name));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        // TODO: temporary until listByNameAndType() works with NS records where
        // name = zoneName
        return Iterators.tryFind(list(), and(nameEqualTo(name), typeEqualTo(type)));
    }

    private List<ResourceRecordDetail> referencesByNameAndType(final String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        final int typeValue = checkNotNull(new ResourceTypeToValue().get(type), "typeValue for %s", type);
        // TODO: temporary until listByNameAndType() works with NS records where
        // name = zoneName
        return api.list().filter(new Predicate<ResourceRecordDetail>() {
            public boolean apply(ResourceRecordDetail in) {
                return name.equals(in.getRecord().getName()) && typeValue == in.getRecord().getType();
            }
        }).toSortedList(byNameTypeAndCreateDate);
    }

    private static final int defaultTTL = 300;

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);

        Optional<Integer> ttlToApply = rrset.getTTL();

        List<ResourceRecordDetail> references = referencesByNameAndType(rrset.getName(), rrset.getType());

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset);

        for (ResourceRecordDetail reference : references) {
            ResourceRecord record = reference.getRecord();
            if (!ttlToApply.isPresent())
                ttlToApply = Optional.of(record.getTTL());
            ResourceRecord updateTTL = record.toBuilder().ttl(ttlToApply.or(defaultTTL)).build();

            Map<String, Object> rdata = toRdataMap().apply(record);
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                // all ok.
                if (ttlToApply.get().intValue() == record.getTTL()) {
                    continue;
                }
                // update ttl of rdata in input
                api.update(reference.getGuid(), updateTTL);
            } else if (ttlToApply.get().intValue() != record.getTTL()) {
                // update ttl of other record
                api.update(reference.getGuid(), updateTTL);
            }
        }
        create(rrset.getName(), rrset.getType(), ttlToApply.or(defaultTTL), recordsLeftToCreate);
    }

    @Override
    public void applyTTLToNameAndType(int ttl, String name, String type) {
        checkNotNull(ttl, "ttl");

        List<ResourceRecordDetail> references = referencesByNameAndType(name, type);
        if (references.isEmpty())
            return;

        for (ResourceRecordDetail reference : references) {
            ResourceRecord updateTTL = reference.getRecord().toBuilder().ttl(ttl).build();
            // this will update normal or RR pool records.
            api.update(reference.getGuid(), updateTTL);
        }
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.getTTL().or(defaultTTL);

        List<ResourceRecordDetail> references = referencesByNameAndType(rrset.getName(), rrset.getType());

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset);

        for (ResourceRecordDetail reference : references) {
            ResourceRecord record = reference.getRecord();
            Map<String, Object> rdata = toRdataMap().apply(record);
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                // all ok.
                if (ttlToApply == record.getTTL()) {
                    continue;
                }
                // update ttl of rdata in input
                api.update(reference.getGuid(), record.toBuilder().ttl(ttlToApply).build());
            } else {
                remove(rrset.getName(), rrset.getType(), reference.getGuid());
            }
        }

        create(rrset.getName(), rrset.getType(), ttlToApply, recordsLeftToCreate);
    }

    private void create(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
        if (rdatas.size() > 0) {
            // adding requires the use of a special RR pool api, however we can
            // update them using the normal one..
            if (roundRobinPoolApi.isPoolType(type)) {
                roundRobinPoolApi.add(name, type, ttl, rdatas);
            } else {
                ResourceRecord.Builder builder = ResourceRecord.rrBuilder()
                                                               .name(name)
                                                               .type(new ResourceTypeToValue().get(type))
                                                               .ttl(ttl);

                for (Map<String, Object> rdata : rdatas) {
                    api.create(builder.rdata(rdata.values()).build());
                }
            }
        }
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);

        for (ResourceRecordDetail reference : referencesByNameAndType(rrset.getName(), rrset.getType())) {
            ResourceRecord record = reference.getRecord();
            if (rrset.contains(toRdataMap().apply(record))) {
                remove(rrset.getName(), rrset.getType(), reference.getGuid());
            }
        }
    }

    private void remove(String name, String type, String guid) {
        if (roundRobinPoolApi.isPoolType(type)) {
            roundRobinPoolApi.remove(name, guid);
        } else {
            api.delete(guid);
        }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        for (ResourceRecordDetail reference : referencesByNameAndType(name, type)) {
            remove(name, type, reference.getGuid());
        }
    }

    private static final Ordering<ResourceRecordDetail> byNameTypeAndCreateDate = new Ordering<ResourceRecordDetail>() {

        @Override
        public int compare(ResourceRecordDetail left, ResourceRecordDetail right) {
            return ComparisonChain.start()
                                  .compare(left.getRecord().getName(), right.getRecord().getName())
                                  .compare(left.getRecord().getType(), right.getRecord().getType())
                                  .compare(left.getCreated(), right.getCreated())
                                  .result();
        }

    };
}
