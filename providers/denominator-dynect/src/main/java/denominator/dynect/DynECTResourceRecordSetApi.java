package denominator.dynect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Ordering.usingToString;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.ResourceRecordSetApi;
import denominator.dynect.DynECTProvider.ReadOnlyApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

public final class DynECTResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    static final class Factory implements denominator.ResourceRecordSetApi.Factory {
        private final DynECTApi api;
        private final ReadOnlyApi allApi;

        @Inject
        Factory(DynECTApi api, ReadOnlyApi allApi) {
            this.api = api;
            this.allApi = allApi;
        }

        @Override
        public ResourceRecordSetApi create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return new DynECTResourceRecordSetApi(api, allApi, idOrName);
        }
    }

    private final DynECTApi api;
    private final ReadOnlyApi allApi;
    private final String zoneFQDN;

    DynECTResourceRecordSetApi(DynECTApi api, ReadOnlyApi allApi, String zoneFQDN) {
        this.api = api;
        this.allApi = allApi;
        this.zoneFQDN = zoneFQDN;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return groupByRecordNameAndType(FluentIterable.from(allApi.recordsInZone(zoneFQDN).values()));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String fqdn) {
        checkNotNull(fqdn, "fqdn was null");
        return groupByRecordNameAndType(FluentIterable.from(allApi.recordsInZoneByName(zoneFQDN, fqdn).values()));
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
            builder.add(numbersToInts(existingRecord));
        }
        return Optional.<ResourceRecordSet<?>> of(builder.ttl(ttl.get()).build());
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.rdata().isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.ttl().or(0);

        List<Record<?>> existingRecords = existing(rrset.name(), rrset.type());

        List<Map<String, Object>> recordsLeftToCreate = Lists.newArrayList(rrset.rdata());

        boolean shouldPublish = false;
        for (Record<?> existingRecord : existingRecords) {
            if (recordsLeftToCreate.contains(existingRecord.getRData())
                    && ttlToApply == existingRecord.getTTL()) {
                recordsLeftToCreate.remove(existingRecord.getRData());
                continue;
            }
            shouldPublish = true;
            api.getRecordApiForZone(zoneFQDN).scheduleDelete(existingRecord);
        }

        if (recordsLeftToCreate.size() > 0) {
            shouldPublish = true;
            CreateRecord.Builder<Map<String, Object>> builder = CreateRecord.builder()
                                                                            .fqdn(rrset.name())
                                                                            .type(rrset.type())
                                                                            .ttl(ttlToApply);
            for (Map<String, Object> record : recordsLeftToCreate) {
                api.getRecordApiForZone(zoneFQDN).scheduleCreate(builder.rdata(record).build());
            }
        }
        if (shouldPublish)
            api.getZoneApi().publish(zoneFQDN);
    }

    ImmutableList<Record<? extends Map<String, Object>>> existing(String name, String type) {
        return allApi.recordsInZoneByNameAndType(zoneFQDN, name, type).toList();
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

    static Map<String, Object> numbersToInts(Record<?> record) {
        return transformValues(record.getRData(), new Function<Object, Object>() {

            @Override
            public Object apply(Object input) {
                // gson makes everything floats, which conflicts with data format for typical types
                // such as MX priority
                return input instanceof Number ? Number.class.cast(input).intValue() : input;
            }

        });
    }
}
