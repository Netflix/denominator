package denominator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.mock.MockProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class ProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testDefaultProviderNameIsLowercase() {
    BareProvider provider = new BareProvider();
    assertThat(provider.credentialTypeToParameterNames()).isEmpty();
  }

  @Test
  public void testCredentialTypeToParameterNamesDefaultsToEmpty() {
    BareProvider provider = new BareProvider();
    assertThat(provider.credentialTypeToParameterNames()).isEmpty();
  }

  @Test
  public void testLowerCamelCredentialTypesAndValuesAreValid() {
    new ValidCredentialParametersProvider();
  }

  @Test
  public void testIllegalArgumentWhenCredentialTypeIsntLowerCamel() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("please correct credential type STS_SESSION to lowerCamel case");

    new InvalidCredentialKeyProvider();
  }

  @Test
  public void testIllegalArgumentWhenCredentialParameterIsntLowerCamel() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "please correct stsSession credential parameter access.key to lowerCamel case");

    new InvalidCredentialParameterProvider();
  }

  static class BareProvider extends BasicProvider {

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static class ValidCredentialParametersProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("accessKey", Arrays.asList("accessKey", "secretKey"));
      options.put("stsSession", Arrays.asList("accessKey", "secretKey", "sessionToken"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static class InvalidCredentialKeyProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("accessKey", Arrays.asList("accessKey", "secretKey"));
      options.put("STS_SESSION", Arrays.asList("accessKey", "secretKey", "sessionToken"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static class InvalidCredentialParameterProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
      options.put("accessKey", Arrays.asList("accessKey", "secretKey"));
      options.put("stsSession", Arrays.asList("access.key", "secret.key", "session.token"));
      return options;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }
}
