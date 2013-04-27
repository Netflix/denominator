package denominator;

import static org.testng.Assert.assertEquals;

import javax.inject.Singleton;

import org.testng.annotations.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials.AnonymousCredentials;
import denominator.Credentials.ListCredentials;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyNormalResourceRecordSets;
import denominator.mock.MockResourceRecordSetApi;
import denominator.mock.MockZoneApi;
import denominator.model.ResourceRecordSet;

@Test
public class CredentialsTest {

    public void testTwoPartCredentialsEqualsHashCode() {
        assertEquals(ListCredentials.from("user", "pass"), ListCredentials.from("user", "pass"));
        assertEquals(ListCredentials.from("user", "pass").hashCode(), ListCredentials.from("user", "pass").hashCode());
    }

    @Module(entryPoints = DNSApiManager.class,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static final class OptionalProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> of();
        }

        @Provides
        ZoneApi provideZoneApi(MockZoneApi zoneApi) {
            return zoneApi;
        }

        @Provides
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
            return in;
        }

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings("rawtypes")
        @Provides
        @Singleton
        Multimap<String, ResourceRecordSet> provideData() {
            return ImmutableMultimap.of();
        }
    }

    @Module(entryPoints = DNSApiManager.class,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static final class TwoPartProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("password", "username", "password").build();
        }

        @Provides
        ZoneApi provideZoneApi(MockZoneApi zoneApi) {
            return zoneApi;
        }

        @Provides
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
            return in;
        }

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings("rawtypes")
        @Provides
        @Singleton
        Multimap<String, ResourceRecordSet> provideData() {
            return ImmutableMultimap.of();
        }
    }

    @Module(entryPoints = DNSApiManager.class,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static final class ThreePartProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("password", "customer", "username", "password").build();
        }

        @Provides
        ZoneApi provideZoneApi(MockZoneApi zoneApi) {
            return zoneApi;
        }

        @Provides
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
            return in;
        }

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings("rawtypes")
        @Provides
        @Singleton
        Multimap<String, ResourceRecordSet> provideData() {
            return ImmutableMultimap.of();
        }
    }

    @Module(entryPoints = DNSApiManager.class,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static final class MultiPartProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("session", "accessKey", "secretKey", "sessionToken").build();
        }

        @Provides
        ZoneApi provideZoneApi(MockZoneApi zoneApi) {
            return zoneApi;
        }

        @Provides
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(MockResourceRecordSetApi.Factory in) {
            return in;
        }

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings("rawtypes")
        @Provides
        @Singleton
        Multimap<String, ResourceRecordSet> provideData() {
            return ImmutableMultimap.of();
        }
    }

    static final Provider OPTIONAL_PROVIDER = new OptionalProvider();

    static final Provider TWO_PART_PROVIDER = new TwoPartProvider();

    static final Provider THREE_PART_PROVIDER = new ThreePartProvider();

    static final Provider MULTI_PART_PROVIDER = new MultiPartProvider();

    public void testTwoPartCheckConfiguredIsOptional() {
        assertEquals(CredentialsConfiguration.checkValidForProvider(null, OPTIONAL_PROVIDER), AnonymousCredentials.INSTANCE);
    }

    public void testTwoPartCheckConfiguredSuccess() {
        assertEquals(CredentialsConfiguration.checkValidForProvider(ListCredentials.from("user", "pass"), TWO_PART_PROVIDER),
                ListCredentials.from("user", "pass"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. twopart requires username, password")
    public void testTwoPartCheckConfiguredExceptionMessageOnNullCredentials() {
        CredentialsConfiguration.checkValidForProvider(null, TWO_PART_PROVIDER);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "provider cannot be null")
    public void testTwoPartCheckConfiguredExceptionMessageOnNullProvider() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("user", "pass"), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. threepart requires customer, username, password")
    public void testTwoPartCheckConfiguredFailsOnIncorrectCountForProvider() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("user", "pass"), THREE_PART_PROVIDER);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. twopart requires username, password")
    public void testTwoPartCheckConfiguredFailsOnIncorrectType() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("customer", "user", "pass"), TWO_PART_PROVIDER);
    }

    public void testThreePartCheckConfiguredSuccess() {
        assertEquals(CredentialsConfiguration.checkValidForProvider(ListCredentials.from("customer", "user", "pass"),
                THREE_PART_PROVIDER), ListCredentials.from("customer", "user", "pass"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. threepart requires customer, username, password")
    public void testThreePartCheckConfiguredExceptionMessageOnNullCredentials() {
        CredentialsConfiguration.checkValidForProvider(null, THREE_PART_PROVIDER);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "provider cannot be null")
    public void testThreePartCheckConfiguredExceptionMessageOnNullProvider() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("customer", "user", "pass"), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. twopart requires username, password")
    public void testThreePartCheckConfiguredFailsOnIncorrectCountForProvider() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("customer", "user", "pass"), TWO_PART_PROVIDER);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. threepart requires customer, username, password")
    public void testThreePartCheckConfiguredFailsOnIncorrectType() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("user", "pass"), THREE_PART_PROVIDER);
    }

    public void testMultiPartCheckConfiguredSuccess() {
        assertEquals(CredentialsConfiguration.checkValidForProvider(ListCredentials.from("accessKey", "secretKey"),
                MULTI_PART_PROVIDER), ListCredentials.from("accessKey", "secretKey"));
        assertEquals(CredentialsConfiguration.checkValidForProvider(
                ListCredentials.from("accessKey", "secretKey", "sessionToken"), MULTI_PART_PROVIDER),
                ListCredentials.from("accessKey", "secretKey", "sessionToken"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. multipart requires one of the following forms: when type is accessKey: accessKey, secretKey; session: accessKey, secretKey, sessionToken")
    public void testMultiPartCheckConfiguredExceptionMessageOnNullCredentials() {
        CredentialsConfiguration.checkValidForProvider(null, MULTI_PART_PROVIDER);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "provider cannot be null")
    public void testMultiPartCheckConfiguredExceptionMessageOnNullProvider() {
        CredentialsConfiguration.checkValidForProvider(ListCredentials.from("customer", "user", "pass"), null);
    }

    public void testThreePartCredentialsEqualsHashCode() {
        assertEquals(ListCredentials.from("customer", "user", "pass"), ListCredentials.from("customer", "user", "pass"));
        assertEquals(ListCredentials.from("customer", "user", "pass").hashCode(),
                ListCredentials.from("customer", "user", "pass").hashCode());
    }

    static enum AWSCredentials {
        INSTANCE;
        String getAWSAccessKeyId() {
            return "accessKey";
        }

        String getAWSSecretKey() {
            return "secretKey";
        }
    }

    static class AWSCredentialsProvider {
        AWSCredentials getCredentials() {
            return AWSCredentials.INSTANCE;
        }
    }

    public void testHowToConvertSomethingLikeAmazon() {
        final AWSCredentialsProvider provider = new AWSCredentialsProvider();
        Supplier<Credentials> converter = new Supplier<Credentials>() {
            public Credentials get() {
                AWSCredentials awsCreds = provider.getCredentials();
                return ListCredentials.from(awsCreds.getAWSAccessKeyId(), awsCreds.getAWSSecretKey());
            }
        };
        assertEquals(converter.get(), ListCredentials.from("accessKey", "secretKey"));
    }
}
