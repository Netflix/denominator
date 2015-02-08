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

    assertThat(name)
        .hasName("denominator.io.")
        .hasNullId()
        .isEqualTo(new Zone("denominator.io.", null));

    assertThat(name.hashCode()).isEqualTo(new Zone("denominator.io.", null).hashCode());
    assertThat(name.toString()).isEqualTo("Zone [name=denominator.io.]");

    Zone id = Zone.create("denominator.io.", "ABCD");

    assertThat(id)
        .hasName("denominator.io.")
        .hasId("ABCD")
        .isEqualTo(new Zone("denominator.io.", "ABCD"))
        .isNotEqualTo(name);

    assertThat(id.hashCode())
        .isEqualTo(new Zone("denominator.io.", "ABCD").hashCode())
        .isNotEqualTo(name.hashCode());

    assertThat(id.toString()).isEqualTo("Zone [name=denominator.io., id=ABCD]");
  }

  @Test
  public void nullNameNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("name");

    new Zone(null, "id");
  }
}
