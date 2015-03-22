package denominator.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Objects;

import denominator.model.Zone;

public class ZoneAssert extends AbstractAssert<ZoneAssert, Zone> {

  Objects objects = Objects.instance();

  public ZoneAssert(Zone actual) {
    super(actual, ZoneAssert.class);
  }

  public ZoneAssert hasName(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.name(), expected);
    return this;
  }

  public ZoneAssert hasId(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.id(), expected);
    return this;
  }

  public ZoneAssert hasQualifier(String expected) {
    isNotNull();
    objects.assertEqual(info, actual.qualifier(), expected);
    return this;
  }

  public ZoneAssert hasNoQualifier() {
    isNotNull();
    objects.assertNull(info, actual.qualifier());
    return this;
  }
}
