package denominator.route53;

import java.util.Iterator;

import denominator.model.Zone;
import denominator.route53.Route53.ZoneList;

import static denominator.common.Util.filter;
import static denominator.model.Zones.nameEqualTo;

public final class Route53ZoneApi implements denominator.ZoneApi {

  private final Route53 api;

  Route53ZoneApi(Route53 api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    final ZoneList first = api.listHostedZones();
    if (first.next == null) {
      return first.iterator();
    }
    return new Iterator<Zone>() {
      Iterator<Zone> current = first.iterator();
      String next = first.next;

      @Override
      public boolean hasNext() {
        while (!current.hasNext() && next != null) {
          ZoneList nextPage = api.listHostedZones(next);
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

  /**
   * This implementation assumes that there isn't more than one page of zones with the same name.
   */
  @Override
  public Iterator<Zone> iterateByName(String name) {
    return filter(api.listHostedZonesByName(name).iterator(), nameEqualTo(name));
  }
}
