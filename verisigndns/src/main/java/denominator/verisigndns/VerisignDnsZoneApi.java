package denominator.verisigndns;

import static denominator.common.Util.singletonIterator;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.Zone;
import denominator.verisigndns.VerisignDnsContentHandlers.Page;
import denominator.verisigndns.VerisignDnsEncoder.Paging;

final class VerisignDnsZoneApi implements denominator.ZoneApi {

  private final VerisignDns api;

  @Inject
  VerisignDnsZoneApi(VerisignDns api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    return new Iterator<Zone>() {
      private Paging paging;
      private Iterator<Zone> current;

      private void check() {
        boolean nextPage = false;

        if (paging == null) {
          paging = new Paging(1);
          nextPage = true;
        } else if (!current.hasNext() && paging.nextPage()) {
          nextPage = true;
        }

        if (nextPage) {
          Page<Zone> page = api.getZones(paging);
          paging.setTotal(page.getCount());
          current = page.getList().iterator();
        }
      }

      @Override
      public boolean hasNext() {
        check();
        return current.hasNext();
      }

      @Override
      public Zone next() {
        check();
        return api.getZone(current.next().name());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    Zone zone = null;
    try {
      zone = api.getZone(name);
    } catch (VerisignDnsException e) {
      if (e.code() != VerisignDnsException.DOMAIN_NOT_FOUND) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }

  @Override
  public String put(Zone zone) {
    try {
      api.createZone(zone);
    } catch (VerisignDnsException e) {
      if (e.code() != VerisignDnsException.DOMAIN_ALREADY_EXISTS) {
        throw e;
      }
    }

    api.updateSoa(zone);
    return zone.name();
  }

  @Override
  public void delete(String zone) {
    try {
      api.deleteZone(zone);
    } catch (VerisignDnsException e) {
      if (e.code() != VerisignDnsException.DOMAIN_NOT_FOUND) {
        throw e;
      }
    }
  }
}
