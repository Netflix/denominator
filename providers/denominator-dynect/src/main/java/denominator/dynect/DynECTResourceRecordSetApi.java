package denominator.dynect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.emptyIterator;
import static com.google.common.collect.Iterators.getNext;
import static java.lang.String.format;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

import denominator.ResourceRecordSetApi;
import denominator.dynect.DynECT.Record;
import denominator.model.ResourceRecordSet;
import feign.FeignException;

public final class DynECTResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final DynECT api;
    private final String zone;

    DynECTResourceRecordSetApi(DynECT api, String zone) {
        this.api = api;
        this.zone = zone;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        try {
            return api.rrsetsInZone(zone);
        } catch (FeignException e) {
            if (e.getMessage().indexOf("status 404") != -1) {
                throw new IllegalArgumentException("zone " + zone + " not found", e);
            }
            throw e;
        }
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(final String name) {
        checkNotNull(name, "name");
        return emptyIteratorOn404(new Supplier<Iterator<ResourceRecordSet<?>>>() {
            @Override
            public Iterator<ResourceRecordSet<?>> get() {
                return api.rrsetsInZoneByName(zone, name);
            }
        });
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(final String name, final String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        Iterator<ResourceRecordSet<?>> iterator = emptyIteratorOn404(new Supplier<Iterator<ResourceRecordSet<?>>>() {
            @Override
            public Iterator<ResourceRecordSet<?>> get() {
                return api.rrsetsInZoneByNameAndType(zone, name, type);
            }
        });
        return Optional.<ResourceRecordSet<?>> fromNullable(getNext(iterator, null));
    }

    @Override
    public void put(final ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.rdata().isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.ttl().or(0);

        List<Map<String, Object>> recordsLeftToCreate = Lists.newArrayList(rrset.rdata());

        Iterator<Record> existingRecords = emptyIteratorOn404(new Supplier<Iterator<Record>>() {
            @Override
            public Iterator<Record> get() {
                return api.recordsInZoneByNameAndType(zone, rrset.name(), rrset.type());
            }
        });

        boolean shouldPublish = false;
        while (existingRecords.hasNext()) {
            Record existing = existingRecords.next();
            if (recordsLeftToCreate.contains(existing.rdata) && ttlToApply == existing.ttl) {
                recordsLeftToCreate.remove(existing.rdata);
                continue;
            }
            shouldPublish = true;
            api.scheduleDeleteRecord(format("%sRecord/%s/%s/%s", existing.type, zone, existing.name, existing.id));
        }

        if (recordsLeftToCreate.size() > 0) {
            shouldPublish = true;
            for (Map<String, Object> rdata : recordsLeftToCreate) {
                api.scheduleCreateRecord(zone, rrset.name(), rrset.type(), ttlToApply, rdata);
            }
        }
        if (shouldPublish)
            api.publish(zone);
    }

    @Override
    public void deleteByNameAndType(final String name, final String type) {
        Iterator<String> existingRecords = emptyIteratorOn404(new Supplier<Iterator<String>>() {
            @Override
            public Iterator<String> get() {
                return api.recordIdsInZoneByNameAndType(zone, name, type).iterator();
            }
        });

        if (!existingRecords.hasNext())
            return;
        boolean shouldPublish = false;
        while (existingRecords.hasNext()) {
            shouldPublish = true;
            api.scheduleDeleteRecord(existingRecords.next());
        }
        if (shouldPublish)
            api.publish(zone);
    }

    static <X> Iterator<X> emptyIteratorOn404(Supplier<Iterator<X>> supplier) {
        try {
            return supplier.get();
        } catch (FeignException e) {
            if (e.getMessage().indexOf("status 404") != -1) {
                return emptyIterator();
            }
            throw e;
        }
    }

    static final class Factory implements denominator.ResourceRecordSetApi.Factory {
        private final DynECT api;

        @Inject
        Factory(DynECT api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return new DynECTResourceRecordSetApi(api, idOrName);
        }
    }
}
