package denominator.clouddns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Wire;

public class CloudDNSConnection {

    final DNSApiManager manager;
    final String mutableZone;

    CloudDNSConnection() {
        String username = emptyToNull(getProperty("clouddns.username"));
        String apiKey = emptyToNull(getProperty("clouddns.apiKey"));
        if (username != null && apiKey != null) {
            CloudDNSProvider provider = new CloudDNSProvider(emptyToNull(getProperty("clouddns.url")));
            @Module(overrides = true)
            class Overrides {
                @Provides
                @Singleton
                Wire provideWire() {
                    return new Wire.LoggingWire().appendToFile("target/test-data/http-wire.log");
                }
            }
            manager = Denominator.create(provider, credentials(username, apiKey), new Overrides());
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("clouddns.zone"));
    }

}
