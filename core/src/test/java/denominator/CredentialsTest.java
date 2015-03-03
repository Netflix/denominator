package denominator;

import org.junit.Test;

import denominator.Credentials.ListCredentials;

import static org.assertj.core.api.Assertions.assertThat;

public class CredentialsTest {

  @Test
  public void testTwoPartCredentialsEqualsHashCode() {
    Credentials original = ListCredentials.from("user", "pass");
    Credentials copy = ListCredentials.from("user", "pass");

    assertThat(original).isEqualTo(copy);
    assertThat(original.hashCode()).isEqualTo(copy.hashCode());
  }

  @Test
  public void testThreePartCredentialsEqualsHashCode() {
    Credentials original = ListCredentials.from("customer", "user", "pass");
    Credentials copy = ListCredentials.from("customer", "user", "pass");

    assertThat(original).isEqualTo(copy);
    assertThat(original.hashCode()).isEqualTo(copy.hashCode());
  }
}
