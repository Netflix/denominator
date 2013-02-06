package denominator.dynect;

import java.io.Closeable;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.DynECTAsyncApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.rest.RestContext;

import com.google.common.base.Supplier;

import dagger.Module;
import dagger.Provides;
import denominator.Backend;
import denominator.Connection;
import denominator.Edge;

@Module(entryPoints = Connection.class)
public class DynECTBackend extends Backend {

    @Override
    public String getName() {
        return "dynect";
    }

    @Provides
    @Singleton
    Edge provideZoneApi(RestContext<DynECTApi, DynECTAsyncApi> context) {
        return new DynECTEdge(new DynECTZoneApi(context.getApi()));
    }

    @Provides
    @Singleton
    Supplier<Credentials> provideCredentials() {
        return new Supplier<Credentials>() {
            public Credentials get() {
                throw new UnsupportedOperationException("TODO: configuration");
            }
        };
    }

    @Provides
    @Singleton
    RestContext<DynECTApi, DynECTAsyncApi> provideContext(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new DynECTProviderMetadata()).credentialsSupplier(credentials).build();
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<DynECTApi, DynECTAsyncApi> context) {
        return context;
    }

}
