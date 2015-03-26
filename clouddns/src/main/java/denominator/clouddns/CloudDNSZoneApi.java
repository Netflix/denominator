package denominator.clouddns;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.model.Zone;

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
    return singletonIterator(zipWithDomain(zones.get(0)));
  }

  /**
   * CloudDNS only exposes a domain's ttl in the show api.
   */
  private Zone zipWithDomain(Zone next) {
    int ttl = api.domain(next.id()).ttl();
    return Zone.builder()
        .name(next.name())
        .id(next.id())
        .email(next.email())
        .ttl(ttl).build();
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
      return zipWithDomain(list.get(i++));
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
