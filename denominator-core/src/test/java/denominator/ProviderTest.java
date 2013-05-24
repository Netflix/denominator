package denominator;

import static org.testng.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import dagger.ObjectGraph;
import dagger.Provides;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyNormalResourceRecordSets;
import denominator.mock.MockResourceRecordSetApi;
import denominator.mock.MockZoneApi;
import denominator.model.ResourceRecordSet;

@Test
public class ProviderTest {

    static class BareProvider extends BasicProvider {

        @dagger.Module(injects = { Accessor.class, DNSApiManager.class }, 
                       includes = { NothingToClose.class,
                                    GeoUnsupported.class,
                                    OnlyNormalResourceRecordSets.class } )
        static class Module {

            @Provides
            public Provider provider() {
                return new BareProvider();
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
    }

    public void testDefaultProviderNameIsLowercase() {
        BareProvider provider = new BareProvider();
        assertEquals(provider.getName(), "bare");
        assertEquals(provider.getCredentialTypeToParameterNames(), ImmutableMultimap.of());
    }

    public void testCredentialTypeToParameterNamesDefaultsToEmpty() {
        BareProvider provider = new BareProvider();
        assertEquals(provider.getCredentialTypeToParameterNames(), ImmutableMultimap.of());
    }

    static class Accessor {
        @Inject
        Provider provider;
    }

    public void testBindsProvider() {
        BareProvider provider = new BareProvider();
        Accessor accessor = ObjectGraph.create(new BareProvider.Module()).get(Accessor.class);
        assertEquals(accessor.provider, provider);
    }

    static class ValidCredentialParametersProvider extends BasicProvider {

        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("stsSession", "accessKey", "secretKey", "sessionToken").build();
        }

        @dagger.Module(injects = DNSApiManager.class,
                       includes = { NothingToClose.class,
                                    GeoUnsupported.class,
                                    OnlyNormalResourceRecordSets.class } )
        static class Module {
    
            @Provides
            public Provider provider() {
                return new ValidCredentialParametersProvider();
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
    }

    public void testLowerCamelCredentialTypesAndValuesAreValid() {
        new ValidCredentialParametersProvider();
    }

    static class InvalidCredentialKeyProvider extends BasicProvider {

        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("STS_SESSION", "accessKey", "secretKey", "sessionToken").build();
        }

        @dagger.Module(injects = DNSApiManager.class,
                       includes = { NothingToClose.class,
                                    GeoUnsupported.class,
                                    OnlyNormalResourceRecordSets.class } )
        static class Module {

            @Provides
            public Provider provider() {
                return new InvalidCredentialKeyProvider();
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
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct credential type STS_SESSION to lowerCamel case")
    public void testIllegalArgumentWhenCredentialTypeIsntLowerCamel() {
        new InvalidCredentialKeyProvider();
    }

    static class InvalidCredentialParameterProvider extends BasicProvider {

        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("stsSession", "access.key", "secret.key", "session.token").build();
        }

        @dagger.Module(injects = DNSApiManager.class,
                       includes = { NothingToClose.class,
                                    GeoUnsupported.class,
                                    OnlyNormalResourceRecordSets.class } )
        static class Module {

            @Provides
            public Provider provider() {
                return new InvalidCredentialParameterProvider();
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
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct stsSession credential parameter access.key to lowerCamel case")
    public void testIllegalArgumentWhenCredentialParameterIsntLowerCamel() {
        new InvalidCredentialParameterProvider();
    }
}
