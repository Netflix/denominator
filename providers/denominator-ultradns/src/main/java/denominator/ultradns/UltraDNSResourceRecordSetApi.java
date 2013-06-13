package denominator.ultradns;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
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
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import denominator.ResourceRecordSetApi;
import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;

public final class UltraDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final ResourceRecordApi api;
    private final UltraDNSRoundRobinPoolApi roundRobinPoolApi;

    UltraDNSResourceRecordSetApi(ResourceRecordApi api, UltraDNSRoundRobinPoolApi roundRobinPoolApi) {
        this.api = api;
        this.roundRobinPoolApi = roundRobinPoolApi;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        // this will list all basic or RR pool records.
        Iterator<ResourceRecordDetail> orderedRecords = api.list().toSortedList(byNameTypeAndCreateDate).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name");
        Iterator<ResourceRecordDetail> orderedRecords = api.listByName(name)
                .toSortedList(byNameTypeAndCreateDate).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        Iterator<ResourceRecordDetail> orderedRecords = referencesByNameAndType(name, type).iterator();
        Iterator<ResourceRecordSet<?>> rrset = new GroupByRecordNameAndTypeIterator(orderedRecords);
        if (rrset.hasNext())
            return Optional.<ResourceRecordSet<?>> of(rrset.next());
        return Optional.<ResourceRecordSet<?>> absent();
    }

    private List<ResourceRecordDetail> referencesByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        int typeValue = checkNotNull(new ResourceTypeToValue().get(type), "typeValue for %s", type);
        return api.listByNameAndType(name, typeValue).toSortedList(byNameTypeAndCreateDate);
    }

    private static final int defaultTTL = 300;

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.rdata().isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.ttl().or(defaultTTL);

        List<ResourceRecordDetail> references = referencesByNameAndType(rrset.name(), rrset.type());

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
                remove(rrset.name(), rrset.type(), reference.getGuid());
            }
        }

        create(rrset.name(), rrset.type(), ttlToApply, recordsLeftToCreate);
    }

    private void create(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
        if (rdatas.size() > 0) {
            // adding requires the use of a special RR pool api, however we can
            // update them using the basic one..
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
    public void deleteByNameAndType(String name, String type) {
        for (ResourceRecordDetail reference : referencesByNameAndType(name, type)) {
            remove(name, type, reference.getGuid());
        }
    }

    private void remove(String name, String type, String guid) {
        if (roundRobinPoolApi.isPoolType(type)) {
            roundRobinPoolApi.remove(name, guid);
        } else {
            api.delete(guid);
        }
    }

    private static final Ordering<ResourceRecordDetail> byNameTypeAndCreateDate = new Ordering<ResourceRecordDetail>() {

        @Override
        public int compare(ResourceRecordDetail left, ResourceRecordDetail right) {
            return ComparisonChain.start()
                                  .compare(left.getRecord().getName(), right.getRecord().getName())
                                  .compare(left.getRecord().getType(), right.getRecord().getType())
                                  // insertion order attempt
                                  .compare(left.getCreated(), right.getCreated())
                                  // UMP-5803 the order returned in getResourceRecordsOfZoneResponse 
                                  // is different than getResourceRecordsOfDNameByTypeResponse.
                                  // We fallback to ordering by rdata to ensure consistent ordering.
                                  .compare(left.getRecord().getRData().toString(),
                                          right.getRecord().getRData().toString())
                                  .result();
        }

    };

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final UltraDNSWSApi api;

        @Inject
        Factory(UltraDNSWSApi api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(final String idOrName) {
            return new UltraDNSResourceRecordSetApi(api.getResourceRecordApiForZone(idOrName),
                    new UltraDNSRoundRobinPoolApi(api.getRoundRobinPoolApiForZone(idOrName)));
        }
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return iterateByName(name);
    }

    @Override
    @Deprecated
    public void add(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.rdata().isEmpty(), "rrset was empty %s", rrset);

        Optional<Integer> ttlToApply = rrset.ttl();

        List<ResourceRecordDetail> references = referencesByNameAndType(rrset.name(), rrset.type());

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
        create(rrset.name(), rrset.type(), ttlToApply.or(defaultTTL), recordsLeftToCreate);
    }

    @Override
    @Deprecated
    public void applyTTLToNameAndType(int ttl, String name, String type) {
        checkNotNull(ttl, "ttl");

        List<ResourceRecordDetail> references = referencesByNameAndType(name, type);
        if (references.isEmpty())
            return;

        for (ResourceRecordDetail reference : references) {
            ResourceRecord updateTTL = reference.getRecord().toBuilder().ttl(ttl).build();
            // this will update basic or RR pool records.
            api.update(reference.getGuid(), updateTTL);
        }
    }

    @Override
    @Deprecated
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.rdata().isEmpty(), "rrset was empty %s", rrset);

        for (ResourceRecordDetail reference : referencesByNameAndType(rrset.name(), rrset.type())) {
            ResourceRecord record = reference.getRecord();
            if (rrset.rdata().contains(toRdataMap().apply(record))) {
                remove(rrset.name(), rrset.type(), reference.getGuid());
            }
        }
    }

    @Override
    @Deprecated
    public void replace(ResourceRecordSet<?> rrset) {
        put(rrset);
    }
}
