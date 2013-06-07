package denominator;

import static com.google.common.base.Objects.toStringHelper;

import java.io.Closeable;
import java.io.IOException;

import javax.inject.Inject;

/**
 * represents the connection between a {@link DNSApi} interface and the
 * {@link Provider} that implements it.
 */
public class DNSApiManager implements Closeable {
    private final Provider provider;
    private final DNSApi api;
    private final Closeable closer;

    @Inject
    DNSApiManager(Provider provider, DNSApi api, Closeable closer) {
        this.provider = provider;
        this.api = api;
        this.closer = closer;
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #api}
     */
    @Deprecated
    public DNSApi getApi() {
        return api;
    }

    /**
     * the currently configured {@link DNSApi}
     */
    public DNSApi api() {
        return api;
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #provider}
     */
    @Deprecated
    public Provider getProvider() {
        return provider;
    }

    /**
     * Get the provider associated with this instance
     */
    public Provider provider() {
        return provider;
    }

    /**
     * closes resources associated with the connections, such as thread pools or
     * open files.
     */
    @Override
    public void close() throws IOException {
        closer.close();
    }
    
    @Override
    public String toString() {
        return toStringHelper(this).add("provider", provider).toString();
    }
}