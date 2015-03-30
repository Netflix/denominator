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
      if (e.code() != UltraDNSException.INVALID_ZONE_NAME) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }

  private Zone fromSOA(String name) {
    List<Record> soa = api.getResourceRecordsOfDNameByType(name, name, 6);
    checkState(!soa.isEmpty(), "SOA record for zone %s was not present", name);
    return Zone.create(name, name, soa.get(0).rdata.get(1));
  }
}
