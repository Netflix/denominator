package denominator.clouddns;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Record;
import denominator.model.Zone;

import static denominator.clouddns.CloudDNSFunctions.awaitComplete;
import static denominator.common.Util.singletonIterator;

class CloudDNSZoneApi implements denominator.ZoneApi {

  private final CloudDNS api;

  @Inject
  CloudDNSZoneApi(CloudDNS api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    return new ZipWithDomain(api.domains());
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    ListWithNext<Zone> zones = api.domainsByName(name);
    if (zones.isEmpty()) {
      return singletonIterator(null);
    }
    return singletonIterator(zipWithSOA(zones.get(0)));
  }

  /**
   * CloudDNS doesn't expose the domain's ttl in the list api.
   */
  private Zone zipWithSOA(Zone next) {
    Record soa = api.recordsByNameAndType(Integer.parseInt(next.id()), next.name(), "SOA").get(0);
    return Zone.create(next.id(), next.name(), soa.ttl, next.email());
  }

  class ZipWithDomain implements Iterator<Zone> {

    ListWithNext<Zone> list;
    int i = 0;
    int length;

    ZipWithDomain(ListWithNext<Zone> list) {
      this.list = list;
      this.length = list.size();
    }

    @Override
    public boolean hasNext() {
      while (i == length && list.next != null) {
        list = api.domains(list.next);
        length = list.size();
        i = 0;
      }
      return i < length;
    }

    @Override
    public Zone next() {
      return zipWithSOA(list.get(i++));
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public String put(Zone zone) {
    if (zone.id() != null) {
      return updateZone(zone.id(), zone);
    }
    try {
      return awaitComplete(api, api.createDomain(zone.name(), zone.email(), zone.ttl()));
    } catch (IllegalStateException e) {
      if (e.getMessage().indexOf("already exists") == -1) {
        throw e;
      }
      String id = api.domainsByName(zone.name()).get(0).id();
      return updateZone(id, zone);
    }
  }

  private String updateZone(String id, Zone zone) {
    awaitComplete(api, api.updateDomain(id, zone.email(), zone.ttl()));
    return id;
  }

  @Override
  public void delete(String id) {
    try {
      awaitComplete(api, api.deleteDomain(id));
    } catch (IllegalStateException e) {
      if (e.getMessage().indexOf("ObjectNotFoundException") == -1) {
        throw e;
      }
    }
  }
}
