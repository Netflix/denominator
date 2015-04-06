package denominator.dynect;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.dynect.DynECT.Record;
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

  @Override
  public String put(Zone zone) {
    try {
      api.createZone(zone.name(), zone.ttl(), zone.email());
    } catch (DynECTException e) {
      if (e.getMessage().indexOf("already exists") == -1) {
        throw e;
      }
      long soaId = getSOA(zone.name()).id;
      api.scheduleUpdateSOA(zone.name(), soaId, zone.ttl(), zone.email());
    }
    api.publish(zone.name());
    return zone.name();
  }

  @Override
  public void delete(String name) {
    try {
      api.deleteZone(name);
    } catch (DynECTException e) {
      if (e.getMessage().indexOf("No such zone") == -1) {
        throw e;
      }
    }
  }

  private Zone fromSOA(String name) {
    Record soa = getSOA(name);
    SOAData soaData = (SOAData) soa.rdata;
    return Zone.create(name, name, soa.ttl, soaData.rname());
  }

  private Record getSOA(String name) {
    Iterator<Record> soa = api.recordsInZoneByNameAndType(name, name, "SOA").data;
    checkState(soa.hasNext(), "SOA record for zone %s was not present", name);
    return soa.next();
  }
}
