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
   * only have multiple results when {@link Provider#supportsDuplicateZoneNames()}.
   *
   * @since 4.5
   */
  Iterator<Zone> iterateByName(String name);

  /**
   * Creates or updates a zone with corresponding to {@link Zone#name()}.
   *
   * <p/> When {@linkplain Provider#supportsDuplicateZoneNames()} and {@link Zone#id()} is set, the
   * corresponding zone will be updated. Otherwise, a new zone will be created.
   *
   * <br> Example adding a zone {@code denominator.io.}:
   *
   * <pre>
   * zoneId = zoneApi.put(Zone.create(null, "denominator.io.", 86400, "nil@denominator.io");
   * </pre>
   *
   * @return the {@link Zone#id() id} of the new or affected zone.
   * @since 4.5
   */
  String put(Zone zone);

  /**
   * Deletes a zone by id idempotently. This does not error if the zone doesn't exist.
   *
   * @param id {@link Zone#id() id} of the zone.
   * @since 4.5
   */
  void delete(String id);
}
