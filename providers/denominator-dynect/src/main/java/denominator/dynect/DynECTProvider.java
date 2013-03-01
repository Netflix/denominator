package denominator.dynect;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.dynect.v3.DynECTAsyncApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.rest.RestContext;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.CredentialsConfiguration.CredentialsAsList;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;

@Module(entryPoints = DNSApiManager.class)
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
    ZoneApi provideZoneApi(RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return new DynECTZoneApi(context.getApi());
    }

    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return new DynECTResourceRecordSetApi.Factory(context.getApi());
    }

    @Provides
    @Singleton
    RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> provideContext(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new DynECTProviderMetadata()).credentialsSupplier(credentials).build();
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return context;
    }
}
