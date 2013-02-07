package denominator.dynect;

import java.io.Closeable;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.dynect.v3.DynECTAsyncApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.rest.RestContext;

import com.google.common.base.Supplier;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApi;
import denominator.DNSApiManager;
import denominator.Provider;

@Module(entryPoints = DNSApiManager.class)
public class DynECTProvider extends Provider {

    @Override
    public String getName() {
        return "dynect";
    }

    @Provides
    @Singleton
    DNSApi provideNetworkServices(RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return new DynECTApi(new DynECTZoneApi(context.getApi()));
    }

    @Provides
    @Singleton
    Supplier<Credentials> supplyCredentials() {
        return new Supplier<Credentials>() {
            public Credentials get() {
                throw new UnsupportedOperationException("TODO: configuration");
            }
        };
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
