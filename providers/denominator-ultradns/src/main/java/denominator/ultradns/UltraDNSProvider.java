package denominator.ultradns;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSProviderMetadata;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CredentialsConfiguration.CredentialsAsList;
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

    @Override
    public Module module() {
        return new Module();
    }
    
    @dagger.Module(injects = DNSApiManager.class,
                   includes = { UltraDNSGeoSupport.class,
                                ConcatNormalAndGeoResourceRecordSets.class })
    final class Module implements Provider.Module {

        @Override
        @Provides
        public Provider provider() {
            return UltraDNSProvider.this;
        }

        @Provides
        @Singleton
        Supplier<Credentials> toJcloudsCredentials(CredentialsAsList supplier) {
            return compose(new ToJcloudsCredentials(), supplier);
        }

        @Provides
        @Singleton
        UltraDNSWSApi provideApi(Supplier<Credentials> credentials) {
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

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            return new Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
    }
}
