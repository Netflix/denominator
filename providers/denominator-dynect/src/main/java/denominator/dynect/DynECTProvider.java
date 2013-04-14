package denominator.dynect;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

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
import denominator.config.GeoUnsupported;

@Module(entryPoints = DNSApiManager.class, includes = GeoUnsupported.class)
public class DynECTProvider extends Provider {

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
        return ImmutableMultimap.<String, String> builder()
                .putAll("password", "customer", "username", "password").build();
    }

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            return new Credentials(creds.get(0) + ":" + creds.get(1), creds.get(2).toString());
        }
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
    DynECTApi provideApi(Supplier<Credentials> credentials) {
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
