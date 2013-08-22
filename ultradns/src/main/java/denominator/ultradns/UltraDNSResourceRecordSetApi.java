package denominator.ultradns;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
        Iterator<Record> orderedRecords = api.getResourceRecordsOfZone(zoneName).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name");
        Iterator<Record> orderedRecords = api.getResourceRecordsOfDNameByType(zoneName, name, 0).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public ResourceRecordSet<?> getByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        Iterator<Record> orderedRecords = recordsByNameAndType(name, type).iterator();
        return nextOrNull(new GroupByRecordNameAndTypeIterator(orderedRecords));
    }

    private List<Record> recordsByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        int typeValue = checkNotNull(lookup(type), "typeValue for %s", type);
        return api.getResourceRecordsOfDNameByType(zoneName, name, typeValue);
    }

    private static final int DEFAULT_TTL = 300;

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;

        List<Record> records = recordsByNameAndType(rrset.name(), rrset.type());

        List<Map<String, Object>> recordsLeftToCreate = new ArrayList<Map<String, Object>>(rrset.records());

        for (Record record : records) {
            Map<String, Object> rdata = forTypeAndRData(rrset.type(), record.rdata);
            if (recordsLeftToCreate.contains(rdata)) {
                recordsLeftToCreate.remove(rdata);
                if (ttlToApply == record.ttl) {
                    continue;
                }
                record.ttl = ttlToApply;
                api.updateResourceRecord(record, zoneName);
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
                    for (Object rdatum : rdata.values()) {
                        record.rdata.add(rdatum.toString());
                    }
                    api.createResourceRecord(record, zoneName);
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
            api.deleteResourceRecord(id);
        } catch (UltraDNSException e) {
            // lost race
            if (e.code() != UltraDNSException.RESOURCE_RECORD_NOT_FOUND)
                throw e;
        }
        if (roundRobinPoolApi.isPoolType(type)) {
            roundRobinPoolApi.deletePool(name, type);
        }
    }

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
