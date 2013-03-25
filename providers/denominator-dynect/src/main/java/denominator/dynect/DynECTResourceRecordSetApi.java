package denominator.dynect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.dynect.GroupByRecordNameAndTypeIterator.getRecord;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.domain.CreateRecord;
import org.jclouds.dynect.v3.domain.Record;
import org.jclouds.dynect.v3.domain.RecordId;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

public final class DynECTResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    static final class Factory implements denominator.ResourceRecordSetApi.Factory {
        private final DynECTApi api;

        @Inject
        Factory(DynECTApi api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(String zoneName) {
            checkNotNull(zoneName, "zoneName was null");
            return new DynECTResourceRecordSetApi(api, zoneName);
        }
    }

    private final DynECTApi api;
    private final String zoneFQDN;

    DynECTResourceRecordSetApi(DynECTApi api, String zoneFQDN) {
        this.api = api;
        this.zoneFQDN = zoneFQDN;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return groupByRecordNameAndType(api.getRecordApiForZone(zoneFQDN).list());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String fqdn) {
        checkNotNull(fqdn, "fqdn was null");
        return groupByRecordNameAndType(api.getRecordApiForZone(zoneFQDN).listByFQDN(fqdn));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        List<Record<?>> existingRecords = existingRecordsByNameAndType(name, type);
        if (existingRecords.isEmpty())
            return Optional.absent();
        
        Optional<Integer> ttl = Optional.absent();
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(name)
                                                                .type(type);

        for (Record<?> existingRecord : existingRecords) {
            if (!ttl.isPresent())
                ttl = Optional.of(existingRecord.getTTL());
            builder.add(existingRecord.getRData());
        }
        return Optional.<ResourceRecordSet<?>> of(builder.ttl(ttl.get()).build());
    }

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);

        Optional<Integer> ttlToApply = rrset.getTTL();

        List<Record<?>> existingRecords = existingRecordsByNameAndType(rrset.getName(), rrset.getType());

        List<Map<String, Object>> recordsLeftToCreate = Lists.newArrayList(rrset);

        for (Record<?> existingRecord : existingRecords) {
            if (!ttlToApply.isPresent())
                ttlToApply = Optional.of(existingRecord.getTTL());
            if (recordsLeftToCreate.contains(existingRecord.getRData())) {
                if (ttlToApply.get().intValue() == existingRecord.getTTL()) {
                    recordsLeftToCreate.remove(existingRecord.getRData());
                    continue;
                }
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
            } else if (ttlToApply.get().intValue() != existingRecord.getTTL()) {
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
                recordsLeftToCreate.add(0, existingRecord.getRData());
            }
        }

        if (recordsLeftToCreate.size() > 0) {
            CreateRecord.Builder<Map<String, Object>> builder = CreateRecord.builder()
                                                                            .fqdn(rrset.getName())
                                                                            .type(rrset.getType())
                                                                            .ttl(ttlToApply.or(0));
            for (Map<String, Object> record : recordsLeftToCreate) {
                api.getRecordApiForZone(zoneFQDN).scheduleCreate(builder.rdata(record).build());
            }
            api.getZoneApi().publish(zoneFQDN);
        }
    }

    @Override
    public void applyTTLToNameAndType(int ttl, String name, String type) {
        checkNotNull(ttl, "ttl");

        List<Record<?>> existingRecords = existingRecordsByNameAndType(name, type);
        if (existingRecords.isEmpty())
            return;

        List<Record<?>> recordsToRecreate = Lists.newArrayList(existingRecords);

        for (Record<?> existingRecord : existingRecords) {
            if (ttl == existingRecord.getTTL()) {
                recordsToRecreate.remove(existingRecord);
                continue;
            }
            api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
        }

        if (recordsToRecreate.size() > 0) {
            CreateRecord.Builder<Map<String, Object>> builder = CreateRecord.builder()
                                                                            .fqdn(name)
                                                                            .type(type)
                                                                            .ttl(ttl);
            for (Record<?> record : recordsToRecreate) {
                api.getRecordApiForZone(zoneFQDN).scheduleCreate(builder.rdata(record.getRData()).build());
            }
            api.getZoneApi().publish(zoneFQDN);
        }
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.getTTL().or(0);

        List<Record<?>> existingRecords = existingRecordsByNameAndType(rrset.getName(), rrset.getType());

        List<Map<String, Object>> recordsLeftToCreate = Lists.newArrayList(rrset);

        for (Record<?> existingRecord : existingRecords) {
            if (recordsLeftToCreate.contains(existingRecord.getRData())
                    && ttlToApply == existingRecord.getTTL()) {
                recordsLeftToCreate.remove(existingRecord.getRData());
                continue;
            }
            api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
        }

        if (recordsLeftToCreate.size() > 0) {
            CreateRecord.Builder<Map<String, Object>> builder = CreateRecord.builder()
                                                                            .fqdn(rrset.getName())
                                                                            .type(rrset.getType())
                                                                            .ttl(ttlToApply);
            for (Map<String, Object> record : recordsLeftToCreate) {
                api.getRecordApiForZone(zoneFQDN).scheduleCreate(builder.rdata(record).build());
            }
            api.getZoneApi().publish(zoneFQDN);
        }
    }

    private List<Record<?>> existingRecordsByNameAndType(String name, String type) {
        return exisingRecordIdsByNameAndType(name, type).transform(new Function<RecordId, Record<?>>() {
            public Record<?> apply(RecordId in) {
                return getRecord(api.getRecordApiForZone(zoneFQDN), in);
            }

            public String toString() {
                return "getRecord()";
            }
        }).filter(notNull()).toSortedList(usingToString());
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);

        List<RecordId> keys = exisingRecordIdsByNameAndType(rrset.getName(), rrset.getType()).toList();
        if (keys.isEmpty())
            return;
        boolean shouldPublish = false;
        for (RecordId key : keys) {
            Record<? extends Map<String, Object>> toEvaluate = getRecord(api.getRecordApiForZone(zoneFQDN), key);
            if (toEvaluate != null && rrset.contains(toEvaluate.getRData())) {
                shouldPublish = true;
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(key);
            }
        }
        if (shouldPublish)
            api.getZoneApi().publish(zoneFQDN);
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        List<RecordId> keys = exisingRecordIdsByNameAndType(name, type).toList();
        if (keys.isEmpty())
            return;
        boolean shouldPublish = false;
        for (RecordId key : keys) {
            shouldPublish = true;
            api.getRecordApiForZone(zoneFQDN).scheduleDelete(key);
        }
        if (shouldPublish)
            api.getZoneApi().publish(zoneFQDN);
    }

    private Iterator<ResourceRecordSet<?>> groupByRecordNameAndType(FluentIterable<RecordId> recordIds) {
        Iterator<RecordId> orderedKeys = recordIds.toSortedList(usingToString()).iterator();
        return filter(new GroupByRecordNameAndTypeIterator(api.getRecordApiForZone(zoneFQDN), orderedKeys), notNull());
    }

    private FluentIterable<RecordId> exisingRecordIdsByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return api.getRecordApiForZone(zoneFQDN).listByFQDNAndType(name, type);
    }
}
