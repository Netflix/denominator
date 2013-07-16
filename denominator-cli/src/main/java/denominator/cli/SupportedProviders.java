package denominator.cli;

import static dagger.Provides.Type.SET;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import denominator.Provider;
import denominator.clouddns.CloudDNSProvider;
import denominator.designate.DesignateProvider;
import denominator.dynect.DynECTProvider;
import denominator.mock.MockProvider;
import denominator.route53.Route53Provider;
import denominator.ultradns.UltraDNSProvider;

@Module(library = true, complete = false)
public class SupportedProviders {
    @Provides(type = SET)
    Provider mock(@Named("url") String url) {
        return new MockProvider(url);
    }

    @Provides(type = SET)
    Provider route53(@Named("url") String url) {
        return new Route53Provider(url);
    }

    @Provides(type = SET)
    Provider ultradns(@Named("url") String url) {
        return new UltraDNSProvider(url);
    }

    @Provides(type = SET)
    Provider dynect(@Named("url") String url) {
        return new DynECTProvider(url);
    }

    @Provides(type = SET)
    Provider clouddns(@Named("url") String url) {
        return new CloudDNSProvider(url);
    }

    @Provides(type = SET)
    Provider designate(@Named("url") String url) {
        return new DesignateProvider(url);
    }
}
