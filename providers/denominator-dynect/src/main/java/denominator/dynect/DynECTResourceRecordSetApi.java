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
import org.jclouds.rest.ResourceNotFoundException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;

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
        Iterator<RecordId> orderedKeys = api.getRecordApiForZone(zoneFQDN).list().toSortedList(usingToString())
                .iterator();
        return filter(new GroupByRecordNameAndTypeIterator(api.getRecordApiForZone(zoneFQDN), orderedKeys), notNull());
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        List<Record<?>> existingRecords = existingRecordsByNameAndType(name, type);
        if (existingRecords.isEmpty())
            return Optional.absent();
        
        Optional<UnsignedInteger> ttl = Optional.absent();
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

        Optional<UnsignedInteger> ttlToApply = rrset.getTTL();

        List<Record<?>> existingRecords = existingRecordsByNameAndType(rrset.getName(), rrset.getType());

        List<Map<String, Object>> recordsLeftToCreate = Lists.newArrayList(rrset);

        for (Record<?> existingRecord : existingRecords) {
            if (!ttlToApply.isPresent())
                ttlToApply = Optional.of(existingRecord.getTTL());
            if (recordsLeftToCreate.contains(existingRecord.getRData())) {
                if (ttlToApply.get().equals(existingRecord.getTTL())) {
                    recordsLeftToCreate.remove(existingRecord.getRData());
                    continue;
                }
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
            } else if (!ttlToApply.get().equals(existingRecord.getTTL())) {
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
                recordsLeftToCreate.add(0, existingRecord.getRData());
            }
        }

        if (recordsLeftToCreate.size() > 0) {
            CreateRecord.Builder<Map<String, Object>> builder = CreateRecord.builder()
                                                                            .fqdn(rrset.getName())
                                                                            .type(rrset.getType())
                                                                            .ttl(ttlToApply.or(UnsignedInteger.ZERO));
            for (Map<String, Object> record : recordsLeftToCreate) {
                api.getRecordApiForZone(zoneFQDN).scheduleCreate(builder.rdata(record).build());
            }
            api.getZoneApi().publish(zoneFQDN);
        }
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);
        UnsignedInteger ttlToApply = rrset.getTTL().or(UnsignedInteger.ZERO);

        List<Record<?>> existingRecords = existingRecordsByNameAndType(rrset.getName(), rrset.getType());

        List<Map<String, Object>> recordsLeftToCreate = Lists.newArrayList(rrset);

        for (Record<?> existingRecord : existingRecords) {
            if (recordsLeftToCreate.contains(existingRecord.getRData()) && ttlToApply.equals(existingRecord.getTTL())) {
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
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        try {
            return api.getRecordApiForZone(zoneFQDN).listByFQDNAndType(name, type)
                    .transform(new Function<RecordId, Record<?>>() {
                        public Record<?> apply(RecordId in) {
                            return getRecord(api.getRecordApiForZone(zoneFQDN), in);
                        }

                        public String toString() {
                            return "getRecord()";
                        }
                    }).filter(notNull()).toSortedList(usingToString());
        } catch (ResourceNotFoundException e) {
            // TODO: fix jclouds to just return an empty set
            return ImmutableList.of();
        }
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);

        List<RecordId> keys = api.getRecordApiForZone(zoneFQDN).listByFQDNAndType(rrset.getName(), rrset.getType())
                .toSortedList(usingToString());
        if (keys.isEmpty())
            return;
        boolean shouldPublish = false;
        for (RecordId key : keys) {
            Record<? extends Map<String, Object>> toEvaluate = api.getRecordApiForZone(zoneFQDN).get(key);
            if (toEvaluate != null && rrset.contains(toEvaluate.getRData())) {
                shouldPublish = true;
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(key);
            }
        }
        if (shouldPublish)
            api.getZoneApi().publish(zoneFQDN);
    }

    @Override
    public void applyTTLToNameAndType(String name, String type, UnsignedInteger ttl) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
