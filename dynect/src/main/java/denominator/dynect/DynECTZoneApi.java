package denominator.dynect;

import java.util.Iterator;

import javax.inject.Inject;

import denominator.model.Zone;

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
}