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

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.CredentialsConfiguration.CredentialsAsList;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;

@Module(entryPoints = DNSApiManager.class)
public class UltraDNSProvider extends Provider {

    @Provides
    protected Provider provideThis() {
        return this;
    }

    @Provides
    @Singleton
    Supplier<Credentials> toJcloudsCredentials(CredentialsAsList supplier) {
        return compose(new ToJcloudsCredentials(), supplier);
    }

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("password", "username", "password").build();
    }

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            return new Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
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
    ZoneApi provideZoneApi(UltraDNSWSApi api) {
        return new UltraDNSZoneApi(api);
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
