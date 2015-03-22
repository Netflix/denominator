package denominator.ultradns;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import denominator.model.Zone;

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
    return api.getZonesOfAccount(account.get()).iterator();
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    Zone zone = null;
    try {
      if (api.getZoneInfo(name).equals(account.get())) {
        zone = Zone.create(name);
      }
    } catch (UltraDNSException e) {
      if (e.code() != UltraDNSException.ZONE_NOT_FOUND) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }
}
