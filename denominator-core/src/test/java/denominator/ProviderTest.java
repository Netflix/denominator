package denominator;

import static org.testng.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
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
    static final String moduleExceptionRegex = "add @Module\\(entryPoints = DNSApiManager.class\\) to denominator.ProviderTest.*";

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "add @Provides to provideThis\\(\\)")
    public void testProvideThisRequiresProvidesAnnotation() {
        new Provider() {
            protected Provider provideThis() {
                return this;
            }
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = moduleExceptionRegex)
    public void testMustAnnotateClassWithModule() {
        new Provider() {
            @Provides
            protected Provider provideThis() {
                return this;
            }
        };
    }

    @Module
    static class ProviderNoEntryPoints extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = moduleExceptionRegex)
    public void testMustAnnotateClassWithEntryPoints() {
        new ProviderNoEntryPoints();
    }

    @Module(entryPoints = String.class)
    static class ProviderWrongEntryPoint extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = moduleExceptionRegex)
    public void testMustAnnotateClassWithDNSApiManagerEntryPoint() {
        new ProviderWrongEntryPoint();
    }

    @Module(entryPoints = { Accessor.class, DNSApiManager.class }, 
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static class BareProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
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
        Accessor accessor = ObjectGraph.create(provider).get(Accessor.class);
        assertEquals(accessor.provider, provider);
    }

    @Module(entryPoints = DNSApiManager.class,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static class ValidCredentialParametersProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("stsSession", "accessKey", "secretKey", "sessionToken").build();
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

    public void testLowerCamelCredentialTypesAndValuesAreValid() {
        new ValidCredentialParametersProvider();
    }

    @Module(entryPoints = DNSApiManager.class,
               includes = { NothingToClose.class,
                            GeoUnsupported.class,
                            OnlyNormalResourceRecordSets.class } )
    static class InvalidCredentialKeyProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("STS_SESSION", "accessKey", "secretKey", "sessionToken").build();
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

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct credential type STS_SESSION to lowerCamel case")
    public void testIllegalArgumentWhenCredentialTypeIsntLowerCamel() {
        new InvalidCredentialKeyProvider();
    }

    @Module(entryPoints = DNSApiManager.class,
             includes = { NothingToClose.class,
                          GeoUnsupported.class,
                          OnlyNormalResourceRecordSets.class } )
    static class InvalidCredentialParameterProvider extends Provider {
        @Provides
        protected Provider provideThis() {
            return this;
        }

        @Override
        public Multimap<String, String> getCredentialTypeToParameterNames() {
            return ImmutableMultimap.<String, String> builder()
                    .putAll("accessKey", "accessKey", "secretKey")
                    .putAll("stsSession", "access.key", "secret.key", "session.token").build();
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

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "please correct stsSession credential parameter access.key to lowerCamel case")
    public void testIllegalArgumentWhenCredentialParameterIsntLowerCamel() {
        new InvalidCredentialParameterProvider();
    }
}
