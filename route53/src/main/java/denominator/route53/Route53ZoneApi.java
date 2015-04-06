package denominator.route53;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import denominator.common.PeekingIterator;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.rdata.SOAData;
import denominator.route53.Route53.ActionOnResourceRecordSet;
import denominator.route53.Route53.HostedZone;
import denominator.route53.Route53.HostedZoneList;
import denominator.route53.Route53.NameAndCount;
import denominator.route53.Route53.ResourceRecordSetList;

import static denominator.common.Preconditions.checkState;
import static denominator.model.ResourceRecordSets.soa;
import static denominator.route53.Route53.ActionOnResourceRecordSet.create;
import static java.util.Arrays.asList;

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

  @Override
  public String put(Zone zone) {
    String name = zone.name();
    String id =
        zone.id() != null ? zone.id()
                          : api.createHostedZone(name, UUID.randomUUID().toString()).get(0).id;
    ResourceRecordSet<SOAData> soa = getSOA(id, name);
    SOAData soaData = soa.records().get(0);
    if (zone.email().equals(soaData.rname()) && zone.ttl() == soa.ttl().intValue()) {
      return id;
    }
    List<ActionOnResourceRecordSet> updates =
        asList(ActionOnResourceRecordSet.delete(soa), create(soa(soa, zone.email(), zone.ttl())));
    api.changeResourceRecordSets(id, updates);
    return id;
  }

  private ResourceRecordSet<SOAData> getSOA(String id, String name) {
    ResourceRecordSetList soa = api.listResourceRecordSets(id, name, "SOA");
    checkState(!soa.isEmpty(), "SOA record for zone %s %s was not present", id, name);
    return (ResourceRecordSet<SOAData>) soa.get(0);
  }

  @Override
  public void delete(String id) {
    try {
      NameAndCount nameAndCount = api.getHostedZone(id);
      if (nameAndCount.resourceRecordSetCount > 2) {
        deleteEverythingExceptNSAndSOA(id, nameAndCount.name);
      }
      api.deleteHostedZone(id);
    } catch (Route53Exception e) {
      if (!e.code().equals("NoSuchHostedZone")) {
        throw e;
      }
    }
  }

  /**
   * Works through the zone, deleting each page of rrsets, except the zone's SOA and the NS rrsets.
   * Once the zone is cleared, it can be deleted.
   *
   * <p/>Users can modify the zone's SOA and NS rrsets, but they cannot be deleted except via
   * deleting the zone.
   */
  private void deleteEverythingExceptNSAndSOA(String id, String name) {
    List<ActionOnResourceRecordSet> deletes = new ArrayList<ActionOnResourceRecordSet>();
    ResourceRecordSetList page = api.listResourceRecordSets(id);
    while (!page.isEmpty()) {
      for (ResourceRecordSet<?> rrset : page) {
        if (rrset.type().equals("SOA") || rrset.type().equals("NS") && rrset.name().equals(name)) {
          continue;
        }
        deletes.add(ActionOnResourceRecordSet.delete(rrset));
      }
      if (!deletes.isEmpty()) {
        api.changeResourceRecordSets(id, deletes);
      }
      if (page.next == null) {
        page.clear();
      } else {
        deletes.clear();
        page = api.listResourceRecordSets(id, page.next.name, page.next.type, page.next.identifier);
      }
    }
  }

  private Zone zipWithSOA(HostedZone next) {
    ResourceRecordSetList soas = api.listResourceRecordSets(next.id, next.name, "SOA");
    checkState(!soas.isEmpty(), "SOA record for zone %s %s was not present", next.id, next.name);
    ResourceRecordSet<SOAData> soa = (ResourceRecordSet<SOAData>) soas.get(0);
    SOAData soaData = soa.records().get(0);
    return Zone.create(next.id, next.name, soa.ttl(), soaData.rname());
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
