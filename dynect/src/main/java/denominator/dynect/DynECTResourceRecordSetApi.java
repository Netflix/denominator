package denominator.dynect;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
            return api.rrsets(zone);
        } catch (FeignException e) {
            if (e.getMessage().indexOf("NOT_FOUND") != -1) {
                throw new IllegalArgumentException("zone " + zone + " not found", e);
            }
            throw e;
        }
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(final String name) {
        checkNotNull(name, "name");
        return emptyIteratorOn404(new Iterable<ResourceRecordSet<?>>() {
            public Iterator<ResourceRecordSet<?>> iterator() {
                return api.rrsetsInZoneByName(zone, name);
            }
        });
    }

    @Override
    public ResourceRecordSet<?> getByNameAndType(final String name, final String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        Iterator<ResourceRecordSet<?>> rrset = emptyIteratorOn404(new Iterable<ResourceRecordSet<?>>() {
            public Iterator<ResourceRecordSet<?>> iterator() {
                return api.rrsetsInZoneByNameAndType(zone, name, type);
            }
        });
        return nextOrNull(rrset);
    }

    @Override
    public void put(final ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);
        int ttlToApply = rrset.ttl() != null ? rrset.ttl() : 0;

        List<Map<String, Object>> recordsLeftToCreate = new ArrayList<Map<String, Object>>(rrset.records());

        Iterator<Record> existingRecords = emptyIteratorOn404(new Iterable<Record>() {
            public Iterator<Record> iterator() {
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
        Iterator<String> existingRecords = emptyIteratorOn404(new Iterable<String>() {
            public Iterator<String> iterator() {
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

    static <X> Iterator<X> emptyIteratorOn404(Iterable<X> supplier) {
        try {
            return supplier.iterator();
        } catch (FeignException e) {
            if (e.getMessage().indexOf("NOT_FOUND") != -1) {
                return Collections.<X> emptyList().iterator();
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
