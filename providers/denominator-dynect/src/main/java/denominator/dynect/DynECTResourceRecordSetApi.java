package denominator.dynect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Ordering.usingToString;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.domain.CreateRecord;
import org.jclouds.dynect.v3.domain.Record;
import org.jclouds.dynect.v3.domain.RecordId;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.ResourceRecordSetApi;
import denominator.dynect.DynECTProvider.ReadOnlyApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

public final class DynECTResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    static final class Factory implements denominator.ResourceRecordSetApi.Factory {
        private final DynECTApi api;
        private final ReadOnlyApi roApi;

        @Inject
        Factory(DynECTApi api, ReadOnlyApi roApi) {
            this.api = api;
            this.roApi = roApi;
        }

        @Override
        public ResourceRecordSetApi create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return new DynECTResourceRecordSetApi(api, roApi, idOrName);
        }
    }

    private final DynECTApi api;
    private final ReadOnlyApi roApi;
    private final String zoneFQDN;

    DynECTResourceRecordSetApi(DynECTApi api, ReadOnlyApi roApi, String zoneFQDN) {
        this.api = api;
        this.roApi = roApi;
        this.zoneFQDN = zoneFQDN;
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return groupByRecordNameAndType(FluentIterable.from(roApi.recordsInZone(zoneFQDN).values()));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String fqdn) {
        checkNotNull(fqdn, "fqdn was null");
        return groupByRecordNameAndType(FluentIterable.from(roApi.recordsInZoneByName(zoneFQDN, fqdn).values()));
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        List<Record<?>> existingRecords = existing(name, type);
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

        List<Record<?>> existingRecords = existing(rrset.getName(), rrset.getType());

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

        List<Record<?>> existingRecords = existing(name, type);
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

        List<Record<?>> existingRecords = existing(rrset.getName(), rrset.getType());

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

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.isEmpty(), "rrset was empty %s", rrset);

        List<Record<?>> existingRecords = existing(rrset.getName(), rrset.getType());
        if (existingRecords.isEmpty())
            return;
        boolean shouldPublish = false;
        for (Record<? extends Map<String, Object>> toEvaluate : existingRecords) {
            if (toEvaluate != null && rrset.contains(toEvaluate.getRData())) {
                shouldPublish = true;
                api.getRecordApiForZone(zoneFQDN).scheduleDelete(toEvaluate);
            }
        }
        if (shouldPublish)
            api.getZoneApi().publish(zoneFQDN);
    }

    ImmutableList<Record<? extends Map<String, Object>>> existing(String name, String type) {
        return roApi.recordsInZoneByNameAndType(zoneFQDN, name, type).toList();
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        List<Record<?>> existingRecords = existing(name, type);
        if (existingRecords.isEmpty())
            return;
        boolean shouldPublish = false;
        for (RecordId key : existingRecords) {
            shouldPublish = true;
            api.getRecordApiForZone(zoneFQDN).scheduleDelete(key);
        }
        if (shouldPublish)
            api.getZoneApi().publish(zoneFQDN);
    }

    private Iterator<ResourceRecordSet<?>> groupByRecordNameAndType(FluentIterable<Record<?>> records) {
        Iterator<Record<?>> ordered = records.toSortedList(usingToString()).iterator();
        return new GroupByRecordNameAndTypeIterator(ordered);
    }
}
