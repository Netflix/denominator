package denominator.designate;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.Zone;

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
}
