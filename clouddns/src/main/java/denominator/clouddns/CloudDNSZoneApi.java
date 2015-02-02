package denominator.clouddns;

import java.net.URI;
import java.util.Iterator;

import javax.inject.Inject;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Pager;
import denominator.model.Zone;

import static denominator.clouddns.RackspaceApis.emptyOn404;

class CloudDNSZoneApi implements denominator.ZoneApi {

  private final CloudDNS api;
  private final Pager<Zone> zonePager = new Pager<Zone>() {

    @Override
    public ListWithNext<Zone> apply(URI nullOrNext) {
      return nullOrNext == null ? api.domains() : api.domains(nullOrNext);
    }

  };

  @Inject
  CloudDNSZoneApi(CloudDNS api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    final ListWithNext<Zone> first = emptyOn404(zonePager, null);
    if (first.next == null) {
      return first.iterator();
    }
    return new Iterator<Zone>() {
      Iterator<Zone> current = first.iterator();
      URI next = first.next;

      @Override
      public boolean hasNext() {
        while (!current.hasNext() && next != null) {
          ListWithNext<Zone> nextPage = emptyOn404(zonePager, next);
          current = nextPage.iterator();
          next = nextPage.next;
        }
        return current.hasNext();
      }

      @Override
      public Zone next() {
        return current.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
