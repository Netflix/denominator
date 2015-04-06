package denominator.ultradns;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import denominator.model.Zone;
import denominator.ultradns.UltraDNS.Record;

import static denominator.common.Preconditions.checkState;
import static denominator.common.Util.singletonIterator;

public final class UltraDNSZoneApi implements denominator.ZoneApi {

  private final UltraDNS api;
  private final Provider<String> account;

  @Inject
  UltraDNSZoneApi(UltraDNS api, @Named("accountID") Provider<String> account) {
    this.api = api;
    this.account = account;
  }

  /**
   * in UltraDNS, zones are scoped to an account.
   */
  @Override
  public Iterator<Zone> iterator() {
    final Iterator<String> delegate = api.getZonesOfAccount(account.get()).iterator();
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
    } catch (UltraDNSException e) {
      if (e.code() != UltraDNSException.ZONE_NOT_FOUND
          && e.code() != UltraDNSException.INVALID_ZONE_NAME) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }

  @Override
  public String put(Zone zone) {
    try {
      api.createPrimaryZone(account.get(), zone.name());
    } catch (UltraDNSException e) {
      if (e.code() != UltraDNSException.ZONE_ALREADY_EXISTS) {
        throw e;
      }
    }
    Record soa = api.getResourceRecordsOfDNameByType(zone.name(), zone.name(), 6).get(0);
    soa.ttl = zone.ttl();
    soa.rdata.set(1, zone.email());
    soa.rdata.set(6, String.valueOf(zone.ttl()));
    api.updateResourceRecord(soa, zone.name());
    return zone.name();
  }

  @Override
  public void delete(String name) {
    try {
      api.deleteZone(name);
    } catch (UltraDNSException e) {
      if (e.code() != UltraDNSException.ZONE_NOT_FOUND) {
        throw e;
      }
    }
  }

  private Zone fromSOA(String name) {
    List<Record> soas = api.getResourceRecordsOfDNameByType(name, name, 6);
    checkState(!soas.isEmpty(), "SOA record for zone %s was not present", name);
    Record soa = soas.get(0);
    return Zone.create(name, name, soa.ttl, soa.rdata.get(1));
  }
}
