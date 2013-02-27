package denominator.mock;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ZoneApi;
import denominator.config.NothingToClose;

/**
 * in-memory {@code Provider}, used for testing.
 */
@Module(entryPoints = DNSApiManager.class, includes = NothingToClose.class)
public class MockProvider extends Provider {

    @Provides
    protected Provider provideThis() {
        return this;
    }

    @Provides
    ZoneApi provideZoneApi(MockZoneApi zoneApi) {
        return zoneApi;
    }
}