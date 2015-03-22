package denominator.designate;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.Zone;

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

  /** Designate V1 does not have a filter by name api. */
  @Override
  public Iterator<Zone> iterateByName(String name) {
    return filter(iterator(), nameEqualTo(name));
  }
}
