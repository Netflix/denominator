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
    Zone email = Zone.create("ABCD", "denominator.io.", 1800, "admin@foo.com");

    assertThat(name)
        .hasId("denominator.io.")
        .hasName("denominator.io.")
        .hasTtl(86400)
        .hasEmail("nil@denominator.io")
        .isEqualTo(name)
        .isNotEqualTo(id)
        .isNotEqualTo(email);

    assertThat(name.hashCode())
        .isNotEqualTo(id.hashCode())
        .isNotEqualTo(email.hashCode());
    assertThat(name.toString())
        .isEqualTo("Zone [name=denominator.io., ttl=86400, email=nil@denominator.io.]");

    assertThat(id)
        .hasId("ABCD")
        .hasName("denominator.io.")
        .hasTtl(86400)
        .hasEmail("nil@denominator.io")
        .isEqualTo(id)
        .isNotEqualTo(name)
        .isNotEqualTo(email);

    assertThat(id.hashCode())
        .isNotEqualTo(name.hashCode())
        .isNotEqualTo(email.hashCode());

    assertThat(id.toString()).isEqualTo(
        "Zone [id=ABCD, name=denominator.io., ttl=86400, email=nil@denominator.io.]");

    assertThat(email)
        .hasId("ABCD")
        .hasName("denominator.io.")
        .hasTtl(1800)
        .hasEmail("admin@foo.com")
        .isEqualTo(email)
        .isNotEqualTo(name)
        .isNotEqualTo(id);

    assertThat(email.hashCode())
        .isNotEqualTo(name.hashCode())
        .isNotEqualTo(id.hashCode());

    assertThat(email.toString()).isEqualTo(
        "Zone [id=ABCD, name=denominator.io., ttl=1800, email=admin@foo.com]");
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

    Zone.create(null, "name", 1800, null);
  }

  @Test
  public void negativeTtl() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Invalid ttl value: -1, must be 0-2147483647");

    Zone.create("ABCD", "denominator.io.", -1, "admin@foo.com");
  }
}
