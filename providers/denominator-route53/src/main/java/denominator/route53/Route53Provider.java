package denominator.route53;

import java.io.Closeable;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.route53.AWSRoute53ProviderMetadata;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.RestContext;
import org.jclouds.route53.Route53AsyncApi;

import com.google.common.base.Supplier;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApi;
import denominator.DNSApiManager;
import denominator.Provider;

@Module(entryPoints = DNSApiManager.class)
public class Route53Provider extends Provider {

    @Override
    public String getName() {
        return "route53";
    }

    @Provides
    @Singleton
    DNSApi provideNetworkServices(RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> context) {
        return new Route53Api(new Route53ZoneApi(context.getApi()));
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
    RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> provideContext(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new AWSRoute53ProviderMetadata()).credentialsSupplier(credentials).build();
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> context) {
        return context;
    }
}
