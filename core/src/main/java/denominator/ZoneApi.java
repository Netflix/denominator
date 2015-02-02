package denominator;

import java.util.Iterator;

import denominator.model.Zone;

public interface ZoneApi extends Iterable<Zone> {

  /**
   * Iterates across all zones, returning their name and id (when present). Implementations are lazy
   * when possible.
   */
  @Override
  Iterator<Zone> iterator();
}
