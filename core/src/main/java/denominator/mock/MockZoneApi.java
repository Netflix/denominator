package denominator.mock;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListSet;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.rdata.SOAData;

import static denominator.common.Preconditions.checkState;
import static denominator.common.Util.filter;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.soa;
import static denominator.model.Zones.nameEqualTo;
import static java.util.Arrays.asList;

final class MockZoneApi implements denominator.ZoneApi {

  private static final Comparator<ResourceRecordSet<?>> TO_STRING =
      new Comparator<ResourceRecordSet<?>>() {
        @Override
        public int compare(ResourceRecordSet<?> arg0, ResourceRecordSet<?> arg1) {
          return arg0.toString().compareTo(arg1.toString());
        }
      };

  private final Map<String, Collection<ResourceRecordSet<?>>> data;

  MockZoneApi(Map<String, Collection<ResourceRecordSet<?>>> data) {
    this.data = data;
    put(Zone.create("denominator.io.", "denominator.io.", 86400, "nil@denominator.io."));
  }

  @Override
  public Iterator<Zone> iterator() {
    final Iterator<Entry<String, Collection<ResourceRecordSet<?>>>>
        delegate = data.entrySet().iterator();
    return new Iterator<Zone>() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public Zone next() {
        Entry<String, Collection<ResourceRecordSet<?>>> next = delegate.next();
        String name = next.getKey();
        Iterator<ResourceRecordSet<?>> soas =
            filter(next.getValue().iterator(), nameAndTypeEqualTo(name, "SOA"));

        checkState(soas.hasNext(), "SOA record for zone %s was not present", name);
        ResourceRecordSet<SOAData> soa = (ResourceRecordSet<SOAData>) soas.next();
        SOAData soaData = soa.records().get(0);
        return Zone.create(name, name, soa.ttl(), soaData.rname());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("remove");
      }
    };
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    return filter(iterator(), nameEqualTo(name));
  }

  @Override
  public String put(Zone zone) {
    if (!data.containsKey(zone.name())) {
      Collection<ResourceRecordSet<?>>
          recordsInZone =
          new ConcurrentSkipListSet<ResourceRecordSet<?>>(TO_STRING);
      SOAData soaData = SOAData.builder().mname("ns1." + zone.name()).rname(zone.email())
          .serial(1).refresh(3600).retry(600).expire(604800).minimum(86400).build();
      recordsInZone.add(ResourceRecordSet.builder()
                            .type("SOA")
                            .name(zone.name())
                            .ttl(zone.ttl())
                            .add(soaData)
                            .build());
      recordsInZone.add(ns(zone.name(), zone.ttl(), asList("ns1." + zone.name())));
      data.put(zone.name(), recordsInZone);
      return zone.name();
    }
    for (Iterator<ResourceRecordSet<?>> i = data.get(zone.name()).iterator(); i.hasNext();) {
      ResourceRecordSet<?> rrset = i.next();
      if (rrset.type().equals("SOA")) {
        SOAData soaData = (SOAData) rrset.records().get(0);
        if (zone.email().equals(soaData.rname()) && zone.ttl() == rrset.ttl().intValue()) {
          return zone.name();
        }
        i.remove();
        data.get(zone.name()).add(soa(rrset, zone.email(), zone.ttl()));
      }
    }
    return zone.name();
  }

  @Override
  public void delete(String name) {
    data.remove(name);
  }
}
