package denominator.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ZoneTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void factoryMethodsWork() {
    Zone name = Zone.create("denominator.io.");

    assertThat(name.name()).isEqualTo("denominator.io.");
    assertThat(name.id()).isNull();
    assertThat(name).isEqualTo(new Zone("denominator.io.", null));
    assertThat(name.hashCode()).isEqualTo(new Zone("denominator.io.", null).hashCode());
    assertThat(name.toString()).isEqualTo("Zone [name=denominator.io.]");

    Zone id = Zone.create("denominator.io.", "ABCD");

    assertThat(id.name()).isEqualTo("denominator.io.");
    assertThat(id.id()).isEqualTo("ABCD");
    assertThat(id).isEqualTo(new Zone("denominator.io.", "ABCD"));
    assertThat(id.hashCode()).isEqualTo(new Zone("denominator.io.", "ABCD").hashCode());
    assertThat(id.toString()).isEqualTo("Zone [name=denominator.io., id=ABCD]");

    assertThat(name).isNotEqualTo(id);
    assertThat(name.hashCode()).isNotEqualTo(id.hashCode());
  }

  @Test
  public void nullNameNPEMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("name");

    new Zone(null, "id");
  }
}
