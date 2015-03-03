package denominator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.Credentials.AnonymousCredentials;
import denominator.Credentials.ListCredentials;
import denominator.mock.MockProvider;

import static denominator.CredentialsConfiguration.checkValidForProvider;
import static org.assertj.core.api.Assertions.assertThat;

public class CredentialsConfigurationTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  public static final Provider OPTIONAL_PROVIDER = new OptionalProvider();
  public static final Provider TWO_PART_PROVIDER = new TwoPartProvider();
  public static final Provider THREE_PART_PROVIDER = new ThreePartProvider();
  public static final Provider MULTI_PART_PROVIDER = new MultiPartProvider();

  @Test
  public void testTwoPartCheckConfiguredIsOptional() {
    assertThat(checkValidForProvider(null, OPTIONAL_PROVIDER))
        .isEqualTo(AnonymousCredentials.INSTANCE);
  }

  @Test
  public void testTwoPartCheckConfiguredSuccess() {
    assertThat(checkValidForProvider(ListCredentials.from("user", "pass"), TWO_PART_PROVIDER))
        .isEqualTo(ListCredentials.from("user", "pass"));
  }

  @Test
  public void testTwoPartCheckConfiguredExceptionMessageOnNullCredentials() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. twopart requires username,password");

    checkValidForProvider(null, TWO_PART_PROVIDER);
  }

  @Test
  public void testTwoPartCheckConfiguredExceptionMessageOnNullProvider() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("provider cannot be null");

    checkValidForProvider(ListCredentials.from("user", "pass"), null);
  }

  @Test
  public void testTwoPartCheckConfiguredFailsOnIncorrectCountForProvider() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "incorrect credentials supplied. threepart requires customer,username,password");

    checkValidForProvider(ListCredentials.from("user", "pass"), THREE_PART_PROVIDER);
  }

  @Test
  public void testTwoPartCheckConfiguredFailsOnIncorrectType() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. twopart requires username,password");

    checkValidForProvider(ListCredentials.from("customer", "user", "pass"), TWO_PART_PROVIDER);
  }

  @Test
  public void testThreePartCheckConfiguredSuccess() {
    assertThat(checkValidForProvider(ListCredentials.from("customer", "user", "pass"),
                                     THREE_PART_PROVIDER))
        .isEqualTo(ListCredentials.from("customer", "user", "pass"));
  }

  @Test
  public void testThreePartCheckConfiguredExceptionMessageOnNullCredentials() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. threepart requires customer,username,password");

    checkValidForProvider(null, THREE_PART_PROVIDER);
  }

  @Test
  public void testThreePartCheckConfiguredExceptionMessageOnNullProvider() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("provider cannot be null");

    checkValidForProvider(ListCredentials.from("customer", "user", "pass"), null);
  }

  @Test
  public void testThreePartCheckConfiguredFailsOnIncorrectCountForProvider() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. twopart requires username,password");

    checkValidForProvider(ListCredentials.from("customer", "user", "pass"), TWO_PART_PROVIDER);
  }

  @Test
  public void testThreePartCheckConfiguredFailsOnIncorrectType() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "incorrect credentials supplied. threepart requires customer,username,password");

    checkValidForProvider(ListCredentials.from("user", "pass"), THREE_PART_PROVIDER);
  }

  @Test
  public void testMultiPartCheckConfiguredSuccess() {
    assertThat(checkValidForProvider(ListCredentials.from("accessKey", "secretKey"),
                                     MULTI_PART_PROVIDER))
        .isEqualTo(ListCredentials.from("accessKey", "secretKey"));
    assertThat(checkValidForProvider(ListCredentials.from("accessKey", "secretKey", "sessionToken"),
                                     MULTI_PART_PROVIDER))
        .isEqualTo(ListCredentials.from("accessKey", "secretKey", "sessionToken"));
  }

  @Test
  public void testMultiPartCheckConfiguredExceptionMessageOnNullCredentials() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "no credentials supplied. multipart requires one of the following forms: when type is accessKey: accessKey,secretKey; session: accessKey,secretKey,sessionToken");

    checkValidForProvider(null, MULTI_PART_PROVIDER);
  }

  @Test
  public void testMultiPartCheckConfiguredExceptionMessageOnNullProvider() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("provider cannot be null");

    checkValidForProvider(ListCredentials.from("customer", "user", "pass"), null);
  }

  static final class OptionalProvider extends BasicProvider {

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static final class TwoPartProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("username", Arrays.asList("username", "password"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static final class ThreePartProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("username", Arrays.asList("customer", "username", "password"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static final class MultiPartProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("accessKey", Arrays.asList("accessKey", "secretKey"));
      options.put("session", Arrays.asList("accessKey", "secretKey", "sessionToken"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }
}
