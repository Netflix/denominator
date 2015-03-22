package denominator.model;

import denominator.common.Filter;

import static denominator.common.Preconditions.checkNotNull;

/**
 * Static utility methods for {@code Zone} instances.
 */
public final class Zones {

  /**
   * Evaluates to true if the input {@link denominator.model.Zone} exists with {@link
   * denominator.model.Zone#name() name} corresponding to the {@code name} parameter.
   *
   * @param name the {@link denominator.model.Zone#name() name} of the desired zone.
   */
  public static Filter<Zone> nameEqualTo(final String name) {
    checkNotNull(name, "name");
    return new Filter<Zone>() {

      @Override
      public boolean apply(Zone in) {
        return in != null && name.equals(in.name());
      }

      @Override
      public String toString() {
        return "nameEqualTo(" + name + ")";
      }
    };
  }

  private Zones() {
  }
}
