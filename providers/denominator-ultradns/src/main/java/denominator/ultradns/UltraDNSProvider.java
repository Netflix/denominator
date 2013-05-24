package denominator.ultradns;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSProviderMetadata;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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

public class UltraDNSProvider extends BasicProvider {

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("password", "username", "password").build();
    }

    @dagger.Module(injects = DNSApiManager.class,
                   complete = false, // no built-in credentials provider
                   includes = { UltraDNSGeoSupport.class,
                                ConcatNormalAndGeoResourceRecordSets.class })
    public static final class Module {

        @Provides
        public Provider provider() {
            return new UltraDNSProvider();
        }

        @Provides
        @Singleton
        UltraDNSWSApi provideApi(ConvertToJcloudsCredentials credentials) {
            return ContextBuilder.newBuilder(new UltraDNSWSProviderMetadata())
                                 .credentialsSupplier(credentials)
                                 .modules(ImmutableSet.<com.google.inject.Module> of(new SLF4JLoggingModule()))
                                 .buildApi(UltraDNSWSApi.class);
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(UltraDNSWSApi api, Supplier<IdAndName> account) {
            return new UltraDNSZoneApi(api, account);
        }

        @Provides
        @Singleton
        Supplier<IdAndName> account(final UltraDNSWSApi api) {
            return Suppliers.memoize(new Supplier<IdAndName>() {
    
                @Override
                public IdAndName get() {
                    return api.getCurrentAccount();
                }
    
                @Override
                public String toString() {
                    return "accountOf(" + api + ")";
                }
            });
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(UltraDNSWSApi api) {
            return new UltraDNSResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        Closeable provideCloser(UltraDNSWSApi api) {
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
            return new org.jclouds.domain.Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
    }
}
