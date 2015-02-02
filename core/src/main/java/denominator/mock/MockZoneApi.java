package denominator.mock;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public final class MockZoneApi implements denominator.ZoneApi {

  private final Map<Zone, SortedSet<ResourceRecordSet<?>>> data;

  // unbound wildcards are not currently injectable in dagger
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Inject
  MockZoneApi(Map<Zone, SortedSet<ResourceRecordSet>> data) {
    this.data = Map.class.cast(data);
  }

  @Override
  public Iterator<Zone> iterator() {
    return data.keySet().iterator();
  }
}