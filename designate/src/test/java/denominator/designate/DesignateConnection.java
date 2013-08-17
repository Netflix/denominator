package denominator.designate;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

public class DesignateConnection {

    final DNSApiManager manager;
    final String mutableZone;

    DesignateConnection() {
        String tenantId = emptyToNull(getProperty("designate.tenantId"));
        String username = emptyToNull(getProperty("designate.username"));
        String password = emptyToNull(getProperty("designate.password"));
        if (username != null && password != null) {
            manager = create(tenantId, username, password);
        } else {
            manager = null;
        }
        mutableZone = emptyToNull(getProperty("designate.zone"));
    }

    @Module(overrides = true, library = true)
    static class Overrides {
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

    static DNSApiManager create(String tenantId, String username, String password) {
        DesignateProvider provider = new DesignateProvider(emptyToNull(getProperty("designate.url")));
        return Denominator.create(provider, credentials(tenantId, username, password), new Overrides());
    }
}
