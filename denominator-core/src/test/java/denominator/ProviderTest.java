package denominator;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.mock.MockProvider;

@Test
public class ProviderTest {

    static class BareProvider extends BasicProvider {
        @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
        static class Module {
        }
    }

    public void testDefaultProviderNameIsLowercase() {
        BareProvider provider = new BareProvider();
        assertEquals(provider.name(), "bare");
        assertEquals(provider.credentialTypeToParameterNames(), ImmutableMultimap.of());
    }

    public void testCredentialTypeToParameterNamesDefaultsToEmpty() {
        BareProvider provider = new BareProvider();
        assertEquals(provider.credentialTypeToParameterNames(), ImmutableMultimap.of());
    }

    static class ValidCredentialParametersProvider extends BasicProvider {

        @Override
        public Multimap<String, String> credentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("stsSession", "accessKey", "secretKey", "sessionToken").build();
        }

        @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
        static class Module {
        }
    }

    public void testLowerCamelCredentialTypesAndValuesAreValid() {
        new ValidCredentialParametersProvider();
    }

    static class InvalidCredentialKeyProvider extends BasicProvider {

        @Override
        public Multimap<String, String> credentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("STS_SESSION", "accessKey", "secretKey", "sessionToken").build();
        }

        @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
        static class Module {
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct credential type STS_SESSION to lowerCamel case")
    public void testIllegalArgumentWhenCredentialTypeIsntLowerCamel() {
        new InvalidCredentialKeyProvider();
    }

    static class InvalidCredentialParameterProvider extends BasicProvider {

        @Override
        public Multimap<String, String> credentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("stsSession", "access.key", "secret.key", "session.token").build();
        }

        @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
        static class Module {
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct stsSession credential parameter access.key to lowerCamel case")
    public void testIllegalArgumentWhenCredentialParameterIsntLowerCamel() {
        new InvalidCredentialParameterProvider();
    }
}
