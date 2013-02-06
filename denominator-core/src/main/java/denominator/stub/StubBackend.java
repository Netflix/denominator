package denominator.stub;

import java.io.Closeable;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.Backend;
import denominator.Connection;
import denominator.Edge;

@Module(entryPoints = Connection.class)
public class StubBackend extends Backend implements Closeable {

    @Override
    public String getName() {
        return "stub";
    }

    @Provides
    @Singleton
    Edge provideZoneApi() {
        return new StubEdge(new StubZoneApi());
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
