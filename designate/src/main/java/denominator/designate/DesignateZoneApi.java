package denominator.designate;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.Zone;
import feign.FeignException;

import static denominator.common.Util.filter;
import static denominator.model.Zones.nameEqualTo;

class DesignateZoneApi implements denominator.ZoneApi {

  private final Designate api;

  @Inject
  DesignateZoneApi(Designate api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    return api.domains().iterator();
  }

  /**
   * Designate V1 does not have a filter by name api.
   */
  @Override
  public Iterator<Zone> iterateByName(String name) {
    return filter(iterator(), nameEqualTo(name));
  }

  @Override
  public String put(Zone zone) {
    if (zone.id() != null) {
      return api.updateDomain(zone.id(), zone.name(), zone.email(), zone.ttl()).id();
    }
    try {
      return api.createDomain(zone.name(), zone.email(), zone.ttl()).id();
    } catch (FeignException e) {
      if (e.getMessage().indexOf(" 409 ") == -1) {
        throw e;
      }
      String id = iterateByName(zone.name()).next().id();
      return api.updateDomain(id, zone.name(), zone.email(), zone.ttl()).id();
    }
  }

  @Override
  public void delete(String id) {
    try {
      api.deleteDomain(id);
    } catch (FeignException e) {
      if (e.getMessage().indexOf(" 404 ") == -1) {
        throw e;
      }
    }
  }
}
