package denominator.model.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkArgument;

/**
 * Utilities for manipulating record sets that contain a {@link Geo} profile.
 */
public class Geos {

  /**
   * Build a {@link ResourceRecordSet} with additional {@link Geo#regions()}.
   *
   * <br> If the existing record set contained no {@link Geo} configuration, the return value will
   * contain only the regions specified. If the existing record set's regions are identical to
   * {@code regionsToAdd}, {@code existing} will be returned directly.
   *
   * @return {@code existing} if there is no change, adding the regions
   * @throws IllegalArgumentException if {code regionsToAdd} were empty or the {@code existing}
   *                                  rrset did not contain a geo profile.
   */
  public static ResourceRecordSet<?> withAdditionalRegions(ResourceRecordSet<?> existing,
                                                           Map<String, Collection<String>> regionsToAdd)
      throws IllegalArgumentException {
    checkArgument(!regionsToAdd.isEmpty(), "no regions specified");
    checkArgument(existing.geo() != null, "rrset does not include geo configuration: %s", existing);
    Map<String, Collection<String>> regionsToApply =
        new LinkedHashMap<String, Collection<String>>();
    for (Entry<String, Collection<String>> entry : existing.geo().regions().entrySet()) {
      regionsToApply.put(entry.getKey(), entry.getValue());
    }
    for (Entry<String, Collection<String>> entry : regionsToAdd.entrySet()) {
      List<String> updates = new ArrayList<String>();
      if (regionsToApply.containsKey(entry.getKey())) {
        updates.addAll(regionsToApply.get(entry.getKey()));
      }
      updates.addAll(entry.getValue());
      regionsToApply.put(entry.getKey(), updates);
    }
    boolean noop = true;
    for (Entry<String, Collection<String>> entry : regionsToApply.entrySet()) {
      if (!existing.geo().regions().containsKey(entry.getKey())) {
        noop = false;
        break;
      }
      Collection<String> existingTerritories = existing.geo().regions().get(entry.getKey());
      if (!existingTerritories.containsAll(entry.getValue())) {
        noop = false;
        break;
      }
    }
    if (noop) {
      return existing;
    }
    return ResourceRecordSet.<Map<String, Object>>builder()//
        .name(existing.name())//
        .type(existing.type())//
        .qualifier(existing.qualifier())//
        .ttl(existing.ttl())//
        .geo(Geo.create(regionsToApply))//
        .weighted(existing.weighted())//
        .addAll(existing.records()).build();
  }
}
