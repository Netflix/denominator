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
    Zone email = Zone.create("denominator.io.", "ABCD", "admin@foo.com");

    assertThat(name)
        .hasName("denominator.io.")
        .hasId("denominator.io.")
        .isEqualTo(name)
        .isNotEqualTo(id)
        .isNotEqualTo(email);

    assertThat(name.hashCode())
        .isNotEqualTo(id.hashCode())
        .isNotEqualTo(email.hashCode());
    assertThat(name.toString())
        .isEqualTo("Zone [name=denominator.io., email=fake@denominator.io.]");

    assertThat(id)
        .hasName("denominator.io.")
        .hasId("ABCD")
        .isEqualTo(id)
        .isNotEqualTo(name)
        .isNotEqualTo(email);

    assertThat(id.hashCode())
        .isNotEqualTo(name.hashCode())
        .isNotEqualTo(email.hashCode());

    assertThat(id.toString()).isEqualTo(
        "Zone [name=denominator.io., id=ABCD, email=fake@denominator.io.]");

    assertThat(email)
        .hasName("denominator.io.")
        .hasId("ABCD")
        .hasEmail("admin@foo.com")
        .isEqualTo(email)
        .isNotEqualTo(name)
        .isNotEqualTo(id);

    assertThat(email.hashCode())
        .isNotEqualTo(name.hashCode())
        .isNotEqualTo(id.hashCode());

    assertThat(email.toString()).isEqualTo(
        "Zone [name=denominator.io., id=ABCD, email=admin@foo.com]");
  }

  @Test
  public void nullNameNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("name");

    Zone.create(null);
  }

  @Test
  public void nullEmailNameNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("email");

    Zone.create("name", null, null);
  }
}
