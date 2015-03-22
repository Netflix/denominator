package denominator.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.assertj.ModelAssertions.assertThat;

public class ZoneTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void factoryMethodsWork() {
    Zone name = Zone.create("denominator.io.");
    Zone id = Zone.create("denominator.io.", "ABCD");
    Zone qualifier = Zone.create("denominator.io.", "Test-Zone", "ABCD");

    assertThat(name)
        .hasName("denominator.io.")
        .hasNoQualifier()
        .hasId("denominator.io.")
        .isEqualTo(name)
        .isEqualTo(id)
        .isNotEqualTo(qualifier);

    assertThat(name.hashCode())
        .isEqualTo(id.hashCode())
        .isNotEqualTo(qualifier.hashCode());
    assertThat(name.toString()).isEqualTo("Zone [name=denominator.io.]");

    assertThat(id)
        .hasName("denominator.io.")
        .hasNoQualifier()
        .hasId("ABCD")
        .isEqualTo(id)
        .isEqualTo(name)
        .isNotEqualTo(qualifier);

    assertThat(id.hashCode())
        .isEqualTo(name.hashCode())
        .isNotEqualTo(qualifier.hashCode());

    assertThat(id.toString()).isEqualTo("Zone [name=denominator.io., id=ABCD]");

    assertThat(qualifier)
        .hasName("denominator.io.")
        .hasQualifier("Test-Zone")
        .hasId("ABCD")
        .isEqualTo(qualifier)
        .isNotEqualTo(name)
        .isNotEqualTo(id);

    assertThat(qualifier.hashCode())
        .isNotEqualTo(name.hashCode())
        .isNotEqualTo(id.hashCode());

    assertThat(qualifier.toString())
        .isEqualTo("Zone [name=denominator.io., qualifier=Test-Zone, id=ABCD]");
  }

  @Test
  public void nullNameNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("name");

    new Zone(null, null, "id");
  }

  @Test
  public void nullIdNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("id");

    new Zone("name", null, null);
  }
}
