package denominator.clouddns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

public class CloudDNSConnection {

    final DNSApiManager manager;
    final String mutableZone;

    CloudDNSConnection() {
        String username = emptyToNull(getProperty("clouddns.username"));
        String apiKey = emptyToNull(getProperty("clouddns.apiKey"));
        if (username != null && apiKey != null) {
            manager = create(username, apiKey);
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("clouddns.zone"));
    }

    static DNSApiManager create(String username, String apiKey) {
        CloudDNSProvider provider = new CloudDNSProvider(emptyToNull(getProperty("clouddns.url")));
        @Module(overrides = true)
        class Overrides {
            @Provides
            @Singleton
            Logger.Level provideLevel() {
                return Logger.Level.FULL;
            }

            @Provides
            @Singleton
            Logger provideLogger() {
                return new Logger.JavaLogger().appendToFile("build/http-wire.log");
            }
        }
        return Denominator.create(provider, credentials(username, apiKey), new Overrides());
    }

}
