package denominator.dynect;

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

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static java.lang.String.format;

public final class DynECTResourceRecordSetApi implements denominator.ResourceRecordSetApi {

  private final DynECT api;
  private final String zone;

  DynECTResourceRecordSetApi(DynECT api, String zone) {
    this.api = api;
    this.zone = zone;
  }

  static <X> Iterator<X> emptyIteratorOn404(Iterable<X> supplier) {
    try {
      return supplier.iterator();
    } catch (FeignException e) {
      if (e.getMessage().indexOf("NOT_FOUND") != -1) {
        return Collections.<X>emptyList().iterator();
      }
      throw e;
    }
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    try {
      return api.rrsets(zone).data;
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
        return api.rrsetsInZoneByName(zone, name).data;
      }
    });
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(final String name, final String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    Iterator<ResourceRecordSet<?>> rrset = emptyIteratorOn404(new Iterable<ResourceRecordSet<?>>() {
      public Iterator<ResourceRecordSet<?>> iterator() {
        return api.rrsetsInZoneByNameAndType(zone, name, type).data;
      }
    });
    return nextOrNull(rrset);
  }

  @Override
  public void put(final ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);
    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : 0;

    List<Map<String, Object>>
        recordsLeftToCreate =
        new ArrayList<Map<String, Object>>(rrset.records());

    Iterator<Record> existingRecords = emptyIteratorOn404(new Iterable<Record>() {
      public Iterator<Record> iterator() {
        return api.recordsInZoneByNameAndType(zone, rrset.name(), rrset.type()).data;
      }
    });

    boolean shouldPublish = false;
    while (existingRecords.hasNext()) {
      Record existing = existingRecords.next();
      if ((recordsLeftToCreate.contains(existing.rdata) && ttlToApply == existing.ttl)
          // Cannot delete service NS records
          || (rrset.type().equals("NS") && "Primary".equals(existing.serviceClass))
          ) {
        recordsLeftToCreate.remove(existing.rdata);
        continue;
      }
      shouldPublish = true;
      api.scheduleDeleteRecord(
          format("%sRecord/%s/%s/%s", existing.type, zone, existing.name, existing.id));
    }

    if (recordsLeftToCreate.size() > 0) {
      shouldPublish = true;
      for (Map<String, Object> rdata : recordsLeftToCreate) {
        api.scheduleCreateRecord(zone, rrset.name(), rrset.type(), ttlToApply, rdata);
      }
    }
    if (shouldPublish) {
      api.publish(zone);
    }
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    try {
      api.scheduleDeleteRecordsInZoneByNameAndType(zone, name, type);
      api.publish(zone);
    } catch (FeignException e) {
      if (e.getMessage().indexOf("NOT_FOUND") == -1) {
        throw e;
      }
    }
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final DynECT api;

    @Inject
    Factory(DynECT api) {
      this.api = api;
    }

    @Override
    public ResourceRecordSetApi create(String name) {
      checkNotNull(name, "name was null");
      return new DynECTResourceRecordSetApi(api, name);
    }
  }
}
