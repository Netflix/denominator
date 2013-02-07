package denominator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.uniqueIndex;

import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.base.Function;

import dagger.ObjectGraph;
import denominator.mock.MockProvider;

public final class Denominator {

    /**
     * returns the currently configured providers
     */
    public static Iterable<Provider> listProviders() {
        return ServiceLoader.load(Provider.class);
    }

    /**
     * creates a new manager for a provider using its type, such as
     * {@link MockProvider}
     * 
     * @see #listProviders
     */
    public static DNSApiManager create(Provider in) {
        return ObjectGraph.create(in).get(DNSApiManager.class);
    }

    /**
     * creates a new manager for a provider, based on key look up. Ex.
     * {@code mock}
     * 
     * @see Provider#getName()
     * @throws IllegalArgumentException
     *             if the providerName is not configured
     */
    public static DNSApiManager create(String providerName) throws IllegalArgumentException {
        checkNotNull(providerName, "providerName");
        Map<String, Provider> allProvidersByName = uniqueIndex(listProviders(), new Function<Provider, String>() {
            public String apply(Provider input) {
                return input.getName();
            }
        });
        checkArgument(allProvidersByName.containsKey(providerName),
                "provider %s not in set of configured providers: %s", providerName, allProvidersByName.keySet());
        return ObjectGraph.create(allProvidersByName.get(providerName)).get(DNSApiManager.class);
    }
}
