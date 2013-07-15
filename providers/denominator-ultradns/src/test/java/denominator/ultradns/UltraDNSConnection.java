package denominator.ultradns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

public class UltraDNSConnection {

    final DNSApiManager manager;
    final String mutableZone;

    UltraDNSConnection() {
        String username = emptyToNull(getProperty("ultradns.username"));
        String password = emptyToNull(getProperty("ultradns.password"));
        if (username != null && password != null) {
            UltraDNSProvider provider = new UltraDNSProvider(emptyToNull(getProperty("ultradns.url")));
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
            manager = Denominator.create(provider, credentials(username, password), new Overrides());
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("ultradns.zone"));
    }
}
