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

  /**
   * Returns a potentially empty iterator of zones with the supplied {@link Zone#name()}. This can
   * only have multiple results when {@link Provider#zoneIdentification() zone identification} is
   * {@link denominator.model.Zone.Identification#QUALIFIED qualified}.
   *
   * @since 4.5
   */
  Iterator<Zone> iterateByName(String name);
}
