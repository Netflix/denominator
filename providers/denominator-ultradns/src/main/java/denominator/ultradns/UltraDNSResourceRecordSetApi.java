package denominator.ultradns;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static denominator.ResourceTypeToValue.lookup;
import static denominator.ultradns.UltraDNSFunctions.toRdataMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.ultradns.UltraDNS.Record;

final class UltraDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final UltraDNS api;
    private final String zoneName;
    private final UltraDNSRoundRobinPoolApi roundRobinPoolApi;

    UltraDNSResourceRecordSetApi(UltraDNS api, String zoneName, UltraDNSRoundRobinPoolApi roundRobinPoolApi) {
        this.api = api;
        this.zoneName = zoneName;
        this.roundRobinPoolApi = roundRobinPoolApi;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        // this will list all basic or RR pool records.
        Iterator<Record> orderedRecords = byNameTypeAndCreateDate.sortedCopy(api.recordsInZone(zoneName)).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "description");
        Iterator<Record> orderedRecords = byNameTypeAndCreateDate.sortedCopy(
                api.recordsInZoneByNameAndType(zoneName, name, 0)).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "description");
        checkNotNull(type, "type");
        Iterator<Record> orderedRecords = recordsByNameAndType(name, type).iterator();
        Iterator<ResourceRecordSet<?>> rrset = new GroupByRecordNameAndTypeIterator(orderedRecords);
        if (rrset.hasNext())
            return Optional.<ResourceRecordSet<?>> of(rrset.next());
        return Optional.<ResourceRecordSet<?>> absent();
    }

    private List<Record> recordsByNameAndType(String name, String type) {
        checkNotNull(name, "description");
        checkNotNull(type, "type");
        int typeValue = checkNotNull(lookup(type), "typeValue for %s", type);
        return byNameTypeAndCreateDate.sortedCopy(api.recordsInZoneByNameAndType(zoneName, name, typeValue));
    }

    private static final int defaultTTL = 300;

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.rdata().isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.ttl().or(defaultTTL);

        List<Record> records = recordsByNameAndType(rrset.name(), rrset.type());

        List<Map<String, Object>> recordsLeftToCreate = newArrayList(rrset.rdata());

        for (Record record : records) {
            Map<String, Object> rdata = toRdataMap().apply(record);
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                if (ttlToApply == record.ttl) {
                    continue;
                }
                record.ttl = ttlToApply;
                api.updateRecordInZone(record, zoneName);
            } else {
                remove(rrset.name(), rrset.type(), record.id);
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
                Record record = new Record();
                record.name = name;
                record.typeCode = lookup(type);
                record.ttl = ttl;

                for (Map<String, Object> rdata : rdatas) {
                    record.rdata = ImmutableList.copyOf(transform(rdata.values(), toStringFunction()));
                    api.createRecordInZone(record, zoneName);
                }
            }
        }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        for (Record record : recordsByNameAndType(name, type)) {
            remove(name, type, record.id);
        }
    }

    private void remove(String name, String type, String id) {
        try {
            api.deleteRecord(id);
        } catch (UltraDNSException e) {
            // TODO: implement default fallback
            if (e.code() == UltraDNSException.RESOURCE_RECORD_NOT_FOUND)
                throw e;
        }
        if (roundRobinPoolApi.isPoolType(type)) {
            roundRobinPoolApi.deletePool(name, type);
        }
    }

    private static final Ordering<Record> byNameTypeAndCreateDate = new Ordering<Record>() {

        @Override
        public int compare(Record left, Record right) {
            return ComparisonChain.start().compare(left.name, right.name).compare(left.typeCode, right.typeCode)
            // insertion order attempt
                    .compare(left.created, right.created)
                    // UMP-5803 the order returned in
                    // getResourceRecordsOfZoneResponse
                    // is different than
                    // getResourceRecordsOfDNameByTypeResponse.
                    // We fallback to ordering by rdata to ensure consistent
                    // ordering.
                    .compare(left.rdata.toString(), right.rdata.toString()).result();
        }

    };

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final UltraDNS api;

        @Inject
        Factory(UltraDNS api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(String idOrName) {
            return new UltraDNSResourceRecordSetApi(api, idOrName, new UltraDNSRoundRobinPoolApi(api, idOrName));
        }
    }
}
