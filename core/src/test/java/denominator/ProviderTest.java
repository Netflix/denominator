package denominator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Map;

import denominator.mock.MockProvider;

import static org.testng.Assert.assertEquals;

@Test
public class ProviderTest {

  public void testDefaultProviderNameIsLowercase() {
    BareProvider provider = new BareProvider();
    assertEquals(provider.name(), "bare");
    assertEquals(provider.credentialTypeToParameterNames(), ImmutableMap.of());
  }

  public void testCredentialTypeToParameterNamesDefaultsToEmpty() {
    BareProvider provider = new BareProvider();
    assertEquals(provider.credentialTypeToParameterNames(), ImmutableMap.of());
  }

  public void testLowerCamelCredentialTypesAndValuesAreValid() {
    new ValidCredentialParametersProvider();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct credential type STS_SESSION to lowerCamel case")
  public void testIllegalArgumentWhenCredentialTypeIsntLowerCamel() {
    new InvalidCredentialKeyProvider();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct stsSession credential parameter access.key to lowerCamel case")
  public void testIllegalArgumentWhenCredentialParameterIsntLowerCamel() {
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
      return ImmutableMultimap.<String, String>builder()
          .putAll("accessKey", "accessKey", "secretKey")
          .putAll("stsSession", "accessKey", "secretKey", "sessionToken").build().asMap();
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static class InvalidCredentialKeyProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      return ImmutableMultimap.<String, String>builder()
          .putAll("accessKey", "accessKey", "secretKey")
          .putAll("STS_SESSION", "accessKey", "secretKey", "sessionToken").build().asMap();
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }

  static class InvalidCredentialParameterProvider extends BasicProvider {

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
      return ImmutableMultimap.<String, String>builder()
          .putAll("accessKey", "accessKey", "secretKey")
          .putAll("stsSession", "access.key", "secret.key", "session.token").build().asMap();
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }
}
