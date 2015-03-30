package denominator.dynect;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.rdata.SOAData;

import static denominator.common.Preconditions.checkState;
import static denominator.common.Util.singletonIterator;

public final class DynECTZoneApi implements denominator.ZoneApi {

  private final DynECT api;

  @Inject
  DynECTZoneApi(DynECT api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    final Iterator<String> delegate = api.zones().data.iterator();
    return new Iterator<Zone>() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public Zone next() {
        return fromSOA(delegate.next());
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
      zone = fromSOA(name);
    } catch (DynECTException e) {
      if (e.getMessage().indexOf("No such zone") == -1) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }

  private Zone fromSOA(String name) {
    Iterator<ResourceRecordSet<?>> soa = api.rrsetsInZoneByNameAndType(name, name, "SOA").data;
    checkState(soa.hasNext(), "SOA record for zone %s was not present", name);

    SOAData soaData = (SOAData) soa.next().records().get(0);
    return Zone.create(name, name, soaData.rname());
  }
}
