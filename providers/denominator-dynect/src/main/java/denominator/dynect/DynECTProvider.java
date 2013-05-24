package denominator.dynect;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatNormalAndGeoResourceRecordSets;

public class DynECTProvider extends BasicProvider {
    
    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()
                .putAll("password", "customer", "username", "password").build();
    }

    @dagger.Module(injects = DNSApiManager.class, 
                   complete = false, // no built-in credentials provider
                   includes = { DynECTGeoSupport.class, 
                                ConcatNormalAndGeoResourceRecordSets.class })
    public static final class Module {

        @Provides
        public Provider provider() {
            return new DynECTProvider();
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(DynECTApi api) {
            return new DynECTZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(DynECTApi api) {
            return new DynECTResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        DynECTApi provideApi(ConvertToJcloudsCredentials credentials) {
            return ContextBuilder.newBuilder(new DynECTProviderMetadata())
                                 .credentialsSupplier(credentials)
                                 .modules(ImmutableSet.<com.google.inject.Module> of(new SLF4JLoggingModule()))
                                 .buildApi(DynECTApi.class);
        }

        @Provides
        @Singleton
        Closeable provideCloseable(DynECTApi api) {
            return api;
        }
    }

    static final class ConvertToJcloudsCredentials implements Supplier<org.jclouds.domain.Credentials> {
        private javax.inject.Provider<Credentials> provider;

        @Inject
        ConvertToJcloudsCredentials(javax.inject.Provider<Credentials> provider) {
            this.provider = provider;
        }

        @Override
        public org.jclouds.domain.Credentials get() {
            ListCredentials creds = ListCredentials.class.cast(provider.get());
            return new org.jclouds.domain.Credentials(creds.get(0) + ":" + creds.get(1), creds.get(2).toString());
        }
    }
}
