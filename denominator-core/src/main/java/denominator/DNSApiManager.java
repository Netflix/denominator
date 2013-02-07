package denominator;

import java.io.Closeable;
import java.io.IOException;

import javax.inject.Inject;

/**
 * represents the connection between a {@link DNSApi} interface and the
 * {@link Provider} that implements it.
 */
public class DNSApiManager implements Closeable {
    private final DNSApi api;
    private final Closeable closer;

    @Inject
    DNSApiManager(DNSApi api, Closeable closer) {
        this.api = api;
        this.closer = closer;
    }

    /**
     * the currently configured {@link DNSApi}
     */
    public DNSApi getApi() {
        return api;
    }

    /**
     * closes resources associated with the connections, such as thread pools or
     * open files.
     */
    @Override
    public void close() throws IOException {
        closer.close();
    }
}