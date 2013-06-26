package denominator.dynect;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Wire;

public class DynECTConnection {

    final DNSApiManager manager;
    final String mutableZone;

    DynECTConnection() {
        String customer = emptyToNull(getProperty("dynect.customer"));
        String username = emptyToNull(getProperty("dynect.username"));
        String password = emptyToNull(getProperty("dynect.password"));
        if (customer != null && username != null && password != null) {
            DynECTProvider provider = new DynECTProvider(emptyToNull(getProperty("dynect.url")));
            @Module(overrides = true)
            class Overrides {
                @Provides
                @Singleton
                Wire provideWire() {
                    return new Wire.LoggingWire().appendToFile("target/test-data/http-wire.log");
                }
            }
            manager = Denominator.create(provider, credentials(customer, username, password), new Overrides());
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("dynect.zone"));
    }
}