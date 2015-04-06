package denominator.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Objects;
import org.assertj.core.internal.Strings;

import denominator.model.Zone;

public class ZoneAssert extends AbstractAssert<ZoneAssert, Zone> {

  Objects objects = Objects.instance();
  Strings strings = Strings.instance();

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

  /**
   * Tolerates differences when the actual email ends with a trailing dot or when the first
   * {@literal @} with a dot.
   */
  public ZoneAssert hasEmail(String expected) {
    isNotNull();
    String actualEmail = actual.email();
    if (actualEmail.endsWith(".")) {
      actualEmail = actualEmail.substring(0, actualEmail.length() - 1);
    }
    if (actualEmail.indexOf('@') == -1) {
      actualEmail = actualEmail.replaceFirst("\\.", "@");
    }
    strings.assertStartsWith(info, actualEmail, expected);
    return this;
  }

  public ZoneAssert hasTtl(Integer expected) {
    isNotNull();
    objects.assertEqual(info, actual.ttl(), expected);
    return this;
  }
}
