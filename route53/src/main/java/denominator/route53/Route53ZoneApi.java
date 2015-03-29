package denominator.route53;

import java.util.Iterator;

import denominator.common.PeekingIterator;
import denominator.model.Zone;
import denominator.model.rdata.SOAData;
import denominator.route53.Route53.HostedZone;
import denominator.route53.Route53.HostedZoneList;
import denominator.route53.Route53.ResourceRecordSetList;

import static denominator.common.Preconditions.checkState;

public final class Route53ZoneApi implements denominator.ZoneApi {

  private final Route53 api;

  Route53ZoneApi(Route53 api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    return new ZipWithSOA(api.listHostedZones());
  }

  /**
   * This implementation assumes that there isn't more than one page of zones with the same name.
   */
  @Override
  public Iterator<Zone> iterateByName(final String name) {
    final Iterator<HostedZone> delegate = api.listHostedZonesByName(name).iterator();
    return new PeekingIterator<Zone>() {
      @Override
      protected Zone computeNext() {
        if (delegate.hasNext()) {
          HostedZone next = delegate.next();
          if (next.name.equals(name)) {
            return zipWithSOA(next);
          }
        }
        return endOfData();
      }
    };
  }

  private Zone zipWithSOA(HostedZone next) {
    ResourceRecordSetList soa = api.listResourceRecordSets(next.id, next.name, "SOA");
    checkState(!soa.isEmpty(), "SOA record for zone %s %s was not present", next.id, next.name);

    SOAData soaData = (SOAData) soa.get(0).records().get(0);
    return Zone.builder()
        .name(next.name)
        .id(next.id)
        .ttl(soaData.minimum())
        .email(soaData.rname()).build();
  }

  /**
   * For each hosted zone, lazy fetch the corresponding SOA record and zip into a Zone object.
   */
  class ZipWithSOA implements Iterator<Zone> {

    HostedZoneList list;
    int i = 0;
    int length;

    ZipWithSOA(HostedZoneList list) {
      this.list = list;
      this.length = list.size();
    }

    @Override
    public boolean hasNext() {
      while (i == length && list.next != null) {
        list = api.listHostedZones(list.next);
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
}
