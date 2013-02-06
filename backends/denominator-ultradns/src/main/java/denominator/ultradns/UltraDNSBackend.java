package denominator.ultradns;

import java.io.Closeable;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.RestContext;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSAsyncApi;
import org.jclouds.ultradns.ws.UltraDNSWSProviderMetadata;

import com.google.common.base.Supplier;

import dagger.Module;
import dagger.Provides;
import denominator.Backend;
import denominator.Connection;
import denominator.Edge;

@Module(entryPoints = Connection.class)
public class UltraDNSBackend extends Backend {

    @Override
    public String getName() {
        return "ultradns";
    }

    @Provides
    @Singleton
    Edge provideZoneApi(RestContext<UltraDNSWSApi, UltraDNSWSAsyncApi> context) {
        return new UltraDNSEdge(new UltraDNSZoneApi(context.getApi()));
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
    RestContext<UltraDNSWSApi, UltraDNSWSAsyncApi> provideContext(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new UltraDNSWSProviderMetadata()).credentialsSupplier(credentials).build();
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<UltraDNSWSApi, UltraDNSWSAsyncApi> context) {
        return context;
    }
}
