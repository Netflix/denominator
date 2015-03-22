package denominator.model;

import org.junit.Test;

import static denominator.model.Zones.nameEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZonesTest {

  Zone zone = Zone.create("denominator.io.");

  @Test
  public void nameEqualToReturnsFalseOnNull() {
    assertFalse(nameEqualTo(zone.name()).apply(null));
  }

  @Test
  public void nameEqualToReturnsFalseOnDifferentName() {
    assertFalse(nameEqualTo("denominator.io").apply(zone));
  }

  @Test
  public void nameEqualToReturnsTrueOnSameName() {
    assertTrue(nameEqualTo(zone.name()).apply(zone));
  }
}
