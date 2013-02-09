package denominator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Maps.uniqueIndex;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import dagger.ObjectGraph;
import denominator.CredentialsConfiguration.CredentialsSupplier;
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
     * {@link MockProvider}.
     * 
     * ex.
     * 
     * <pre>
     * route53 = Denominator.create(new Route53Provider(), staticCredentials(accesskey, secretkey));
     * </pre>
     * 
     * @see CredentialsConfiguration
     * @see #listProviders
     */
    public static DNSApiManager create(Provider in, Object... modules) {
        Builder<Object> modulesForGraph = ImmutableList.builder().add(in);
        List<Object> inputModules;
        if (modules == null || modules.length == 0) {
            inputModules = ImmutableList.of();
        } else {
            inputModules = ImmutableList.copyOf(modules);
        }
        if (!any(inputModules, instanceOf(CredentialsSupplier.class)))
            modulesForGraph.add(CredentialsConfiguration.none());
        modulesForGraph.addAll(inputModules);
        return ObjectGraph.create(modulesForGraph.build().toArray()).get(DNSApiManager.class);
    }

    /**
     * creates a new manager for a provider, based on key look up. Ex.
     * {@code mock}
     * 
     * ex.
     * 
     * <pre>
     * route53 = Denominator.create(&quot;route53&quot;, staticCredentials(accesskey, secretkey));
     * </pre>
     * 
     * @see Provider#getName()
     * @see CredentialsConfiguration
     * @throws IllegalArgumentException
     *             if the providerName is not configured
     */
    public static DNSApiManager create(String providerName, Object... modules) throws IllegalArgumentException {
        checkNotNull(providerName, "providerName");
        Map<String, Provider> allProvidersByName = uniqueIndex(listProviders(), new Function<Provider, String>() {
            public String apply(Provider input) {
                return input.getName();
            }
        });
        checkArgument(allProvidersByName.containsKey(providerName),
                "provider %s not in set of configured providers: %s", providerName, allProvidersByName.keySet());
        return create(allProvidersByName.get(providerName), modules);
    }

}
