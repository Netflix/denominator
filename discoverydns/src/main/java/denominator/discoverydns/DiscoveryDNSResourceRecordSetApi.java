package denominator.discoverydns;

import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.filter;

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

public class DiscoveryDNSResourceRecordSetApi implements ResourceRecordSetApi {
    private String zone;
    private DiscoveryDNS api;

    DiscoveryDNSResourceRecordSetApi(String zone, DiscoveryDNS api) {
        this.zone = zone;
        this.api = api;
    }

    public String getZoneId(String zone) {
        DiscoveryDNS.Zones zones = api.findZone(zone);
        return zones.zones.zoneList.iterator().next().id;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        String zoneId = getZoneId(zone);
        Set<ResourceRecordSet<?>> records = api.getZone(zoneId).zone.resourceRecords.records;
        return records.iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        String zoneId = getZoneId(zone);
        Set<ResourceRecordSet<?>> records = api.getZone(zoneId).zone.resourceRecords.records;
        return filter(records.iterator(), nameEqualTo(name));
    }

    @Override
    public ResourceRecordSet<?> getByNameAndType(String name, String type) {
        String zoneId = getZoneId(zone);
        Set<ResourceRecordSet<?>> records = api.getZone(zoneId).zone.resourceRecords.records;
        return nextOrNull(filter(records.iterator(), nameAndTypeEqualTo(name, type)));
    }

    private void updateResourceRecords(String zone, final String name, final String type, ResourceRecordSet<?>... appends) {
        String zoneId = getZoneId(zone);

        DiscoveryDNS.Zone ddnsZone = api.getZone(zoneId);
        Set<ResourceRecordSet<?>> records = new LinkedHashSet<ResourceRecordSet<?>>();
        for (ResourceRecordSet<?> record : ddnsZone.zone.resourceRecords.records) {
            if (!name.equals(record.name()) || !type.equals(record.type()))
                records.add(record);
        }
        if (appends != null) {
            for (ResourceRecordSet<?> append : appends)
                records.add(append);
        }

        DiscoveryDNS.Zone ddnsUpdateZone = new DiscoveryDNS.Zone();
        ddnsUpdateZone.zoneUpdateResourceRecords = ddnsZone.zone;
        ddnsUpdateZone.zoneUpdateResourceRecords.resourceRecords.records = records;
        api.updateZone(zoneId, ddnsUpdateZone);
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        updateResourceRecords(zone, rrset.name(), rrset.type(), rrset);
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        updateResourceRecords(zone, name, type);
    }

    static final class ResourceRecordSetApiFactory implements denominator.ResourceRecordSetApi.Factory {
        private final DiscoveryDNS api;

        @Inject
        ResourceRecordSetApiFactory(DiscoveryDNS api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(String zone) {
            return new DiscoveryDNSResourceRecordSetApi(zone, api);
        }
    }
}
