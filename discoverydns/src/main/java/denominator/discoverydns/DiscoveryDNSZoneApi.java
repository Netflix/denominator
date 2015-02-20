package denominator.discoverydns;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import denominator.ZoneApi;
import denominator.model.Zone;

final class DiscoveryDNSZoneApi implements ZoneApi {

  private final DiscoveryDNS api;

  DiscoveryDNSZoneApi(DiscoveryDNS api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    List<Zone> zones = new LinkedList<Zone>();
    for (DiscoveryDNS.Zones.ZoneList.Zone zone : api.listZones().zones.zoneList) {
      zones.add(Zone.create(zone.name, zone.id));
    }
    return zones.iterator();
  }
}
