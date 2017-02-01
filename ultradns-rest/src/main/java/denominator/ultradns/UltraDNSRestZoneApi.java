package denominator.ultradns;

import denominator.model.Zone;
import denominator.ultradns.model.Record;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;

import static denominator.common.Preconditions.checkState;
import static denominator.common.Util.singletonIterator;

public final class UltraDNSRestZoneApi implements denominator.ZoneApi {

  private final UltraDNSRest api;

  private static final Logger logger = Logger.getLogger(UltraDNSRestZoneApi.class);

  @Inject
  UltraDNSRestZoneApi(UltraDNSRest api) {
    this.api = api;
  }

  /**
   * in UltraDNSRest, zones are scoped to an account.
   */
  @Override
  public Iterator<Zone> iterator() {
    final Iterator<String> delegate = api.getZonesOfAccount(getCurrentAccountName()).getZoneNames().iterator();
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
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_NOT_FOUND
          && e.code() != UltraDNSRestException.INVALID_ZONE_NAME) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }

  @Override
  public String put(Zone zone) {
    try {
      String accountName = getCurrentAccountName();
      logger.info("Creating zone for: " + accountName + " with name: " + zone.name());
      api.createPrimaryZone(zone.name(), accountName, "PRIMARY", false, "NEW");
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_ALREADY_EXISTS) {
        throw e;
      }
    }
    Record soa = api.getResourceRecordsOfDNameByType(zone.name(), zone.name(), 6).buildRecords().get(0);
    soa.ttl = zone.ttl();
    soa.rdata.set(1, zone.email());
    soa.rdata.set(6, String.valueOf(zone.ttl()));
    logger.info(soa.toString());
    //api.updateResourceRecord(soa, zone.name());
    return zone.name();
  }

  @Override
  public void delete(String name) {
    try {
      logger.info("Deleting Zone with name: " + name);
      api.deleteZone(name);
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_NOT_FOUND) {
        throw e;
      }
    }
  }

  private Zone fromSOA(String name) {
    List<Record> soas = api.getResourceRecordsOfDNameByType(name, name, 6).buildRecords();
    checkState(!soas.isEmpty(), "SOA record for zone %s was not present", name);
    Record soa = soas.get(0);
    return Zone.create(name, name, soa.ttl, soa.rdata.get(1));
  }

  private String getCurrentAccountName(){
    return api.getAccountsListOfUser().getAccounts().get(0).getAccountName();
  }
}
