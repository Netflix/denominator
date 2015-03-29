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

    assertThat(name)
        .hasName("denominator.io.")
        .hasId("denominator.io.")
        .isEqualTo(name)
        .isNotEqualTo(id);

    assertThat(name.hashCode())
        .isNotEqualTo(id.hashCode());
    assertThat(name.toString())
        .isEqualTo("Zone [name=denominator.io., email=fake@denominator.io., ttl=86400]");

    assertThat(id)
        .hasName("denominator.io.")
        .hasId("ABCD")
        .isEqualTo(id)
        .isNotEqualTo(name);

    assertThat(id.hashCode())
        .isNotEqualTo(name.hashCode());

    assertThat(id.toString()).isEqualTo(
        "Zone [name=denominator.io., id=ABCD, email=fake@denominator.io., ttl=86400]");
  }

  @Test
  public void nullNameNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("name");

    Zone.builder().build();
  }
}
