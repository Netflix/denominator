package denominator.discoverydns;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;

final class DiscoveryDNSResourceRecordSetApi implements ResourceRecordSetApi {

  private String zoneId;
  private DiscoveryDNS api;

  DiscoveryDNSResourceRecordSetApi(String zoneId, DiscoveryDNS api) {
    this.zoneId = zoneId;
    this.api = api;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    List<ResourceRecordSet<?>> records = api.getZone(zoneId).zone.resourceRecords.records;
    return records.iterator();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    List<ResourceRecordSet<?>> records = api.getZone(zoneId).zone.resourceRecords.records;
    return filter(records.iterator(), nameEqualTo(name));
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    List<ResourceRecordSet<?>> records = api.getZone(zoneId).zone.resourceRecords.records;
    return nextOrNull(filter(records.iterator(), nameAndTypeEqualTo(name, type)));
  }

  private void updateResourceRecords(String zoneId, final String name, final String type,
                                     ResourceRecordSet<?>... appends) {
    DiscoveryDNS.Zone ddnsZone = api.getZone(zoneId);
    List<ResourceRecordSet<?>> records = new ArrayList<ResourceRecordSet<?>>();
    for (ResourceRecordSet<?> record : ddnsZone.zone.resourceRecords.records) {
      if (!name.equals(record.name()) || !type.equals(record.type())) {
        records.add(record);
      }
    }
    if (appends != null) {
      for (ResourceRecordSet<?> append : appends) {
        records.add(append);
      }
    }

    DiscoveryDNS.Zone ddnsUpdateZone = new DiscoveryDNS.Zone();
    ddnsUpdateZone.zoneUpdateResourceRecords = ddnsZone.zone;
    ddnsUpdateZone.zoneUpdateResourceRecords.resourceRecords.records = records;
    api.updateZone(zoneId, ddnsUpdateZone);
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    updateResourceRecords(zoneId, rrset.name(), rrset.type(), rrset);
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    updateResourceRecords(zoneId, name, type);
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final DiscoveryDNS api;

    Factory(DiscoveryDNS api) {
      this.api = api;
    }

    @Override
    public ResourceRecordSetApi create(String zoneId) {
      return new DiscoveryDNSResourceRecordSetApi(zoneId, api);
    }
  }
}
