package denominator.dynect;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

public class DynECTConnection {

    final DNSApiManager manager;
    final String mutableZone;

    DynECTConnection() {
        String customer = emptyToNull(getProperty("dynect.customer"));
        String username = emptyToNull(getProperty("dynect.username"));
        String password = emptyToNull(getProperty("dynect.password"));
        if (customer != null && username != null && password != null) {
            manager = create(customer, username, password);
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("dynect.zone"));
    }

    static DNSApiManager create(String customer, String username, String password) {
        DynECTProvider provider = new DynECTProvider(emptyToNull(getProperty("dynect.url")));
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
        return Denominator.create(provider, credentials(customer, username, password), new Overrides());
    }
}