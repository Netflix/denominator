package denominator.mock;

import java.io.Closeable;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApi;
import denominator.DNSApiManager;
import denominator.Provider;

@Module(entryPoints = DNSApiManager.class)
public class MockProvider extends Provider implements Closeable {

    @Override
    @Provides
    public String getName() {
        return "mock";
    }

    @Provides
    @Singleton
    DNSApi provideNetworkServices() {
        return new MockDNSApi(new MockZoneApi());
    }

    /**
     * in a real implementation, we would likely inject resources that need
     * cleanup and call them here. For example, shutting down thread pools, or
     * syncing disk write.
     */
    @Provides
    @Singleton
    Closeable provideCloser() {
        return this;
    }

    @Override
    public void close() {
        // no need to close anything
    }
}
