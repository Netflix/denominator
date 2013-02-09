package denominator.ultradns;

import static com.google.common.base.Preconditions.checkState;

import java.io.Closeable;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.RestContext;
import org.jclouds.ultradns.ws.UltraDNSWSAsyncApi;
import org.jclouds.ultradns.ws.UltraDNSWSProviderMetadata;

import com.google.common.base.Supplier;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials.TwoPartCredentials;
import denominator.DNSApi;
import denominator.DNSApiManager;
import denominator.Provider;

@Module(entryPoints = DNSApiManager.class)
public class UltraDNSProvider extends Provider {

    @Override
    @Provides
    public String getName() {
        return "ultradns";
    }

    @Provides
    @Singleton
    DNSApi provideNetworkServices(RestContext<org.jclouds.ultradns.ws.UltraDNSWSApi, UltraDNSWSAsyncApi> context) {
        return new UltraDNSApi(new UltraDNSZoneApi(context.getApi()));
    }

    @Provides
    @Singleton
    Supplier<Credentials> supplyJCloudsCredentials(final Supplier<denominator.Credentials> denominatorCredentials) {
        return new Supplier<Credentials>() {
            public Credentials get() {
                denominator.Credentials input = denominatorCredentials.get();
                checkState(input != null && input instanceof TwoPartCredentials,
                        "%s requires credentials with two parts: username and password", getName());
                TwoPartCredentials<?, ?> twoParts = TwoPartCredentials.class.cast(input);
                return new Credentials(twoParts.getFirstPart().toString(), twoParts.getSecondPart().toString());
            }
        };
    }

    @Provides
    @Singleton
    RestContext<org.jclouds.ultradns.ws.UltraDNSWSApi, UltraDNSWSAsyncApi> provideContext(
            Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new UltraDNSWSProviderMetadata()).credentialsSupplier(credentials).build();
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<org.jclouds.ultradns.ws.UltraDNSWSApi, UltraDNSWSAsyncApi> context) {
        return context;
    }
}
