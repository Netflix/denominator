package denominator.dynect;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.Zone;

import static denominator.common.Util.singletonIterator;

public final class DynECTZoneApi implements denominator.ZoneApi {

  private final DynECT api;

  @Inject
  DynECTZoneApi(DynECT api) {
    this.api = api;
  }

  @Override
  public Iterator<Zone> iterator() {
    return api.zones().data.iterator();
  }

  @Override
  public Iterator<Zone> iterateByName(String name) {
    Zone zone = null;
    try {
      api.getZone(name);
      zone = zone.create(name);
    } catch (DynECTException e) {
      if (e.getMessage().indexOf("No such zone") == -1) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }
}
