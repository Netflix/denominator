package denominator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.uniqueIndex;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import dagger.ObjectGraph;

/**
 * Entry-point to create instances of {@link DNSApiManager}
 * 
 * ex.
 * 
 * <pre>
 * ultraDns = Denominator.create(new UltraDNSProvider(), credentials(username, password));
 * </pre>
 * 
 * <h4>Alternative</h4>
 * 
 * This class implies use of reflection to lookup the dagger module associated
 * with the provider. You can alternatively not use this class and instead use
 * dagger directly.
 * 
 * ex.
 * 
 * <pre>
 * ultraDns = ObjectGraph.create(new UltraDNSProvider.Module(), credentials(username, password)).get(DNSApiManager.class);
 * </pre>
 * 
 */
public final class Denominator {

    public static enum Version {
        INSTANCE;

        private final String version;

        private Version() {
            this.version = Version.class.getPackage().getSpecificationVersion();
        }

        @Override
        public String toString() {
            return version;
        }
    }

    /**
     * Returns the currently configured {@link Provider providers} from
     * {@link ServiceLoader#load(Class)}.
     * 
     * <h4>Performance Note</h4>
     * 
     * The implicit call {@link ServiceLoader#load(Class)} can add delays
     * measurable in 10s to hundreds of milliseconds depending on the number of
     * jars in your classpath. If you desire speed, it is best to instantiate
     * Providers directly.
     */
    public static Iterable<Provider> listProviders() {
        return ServiceLoader.load(Provider.class);
    }

    /**
     * Creates a new manager for a provider using its type, such as
     * {@link denominator.mock.MockProvider}.
     * 
     * ex.
     * 
     * <pre>
     * ultraDns = Denominator.create(new UltraDNSProvider(), credentials(username, password));
     * </pre>
     * 
     * @see CredentialsConfiguration
     * @see #listProviders
     * @throws IllegalArgumentException
     *             if the input provider is misconfigured or expects
     *             credentials.
     */
    public static DNSApiManager create(Provider in, Object... modules) {
        Builder<Object> modulesForGraph = ImmutableList.builder().add(instantiateModule(in));
        List<Object> inputModules;
        if (modules == null || modules.length == 0) {
            inputModules = ImmutableList.of();
        } else {
            inputModules = ImmutableList.copyOf(modules);
        }
        modulesForGraph.addAll(inputModules);
        try {
            return ObjectGraph.create(modulesForGraph.build().toArray()).get(DNSApiManager.class);
        } catch (IllegalStateException e) {
            // much simpler to special-case when a credential module is needed,
            // but not supplied, than do too much magic.
            if (e.getMessage().contains("No binding for denominator.Credentials")
                    && !in.getCredentialTypeToParameterNames().isEmpty()) {
                throw new IllegalArgumentException(CredentialsConfiguration.exceptionMessage(null, in));
            }
            throw e;
        }
    }

    private static Object instantiateModule(Provider in) throws IllegalArgumentException {
        String moduleClassName = in.getClass().getName() + "$Module";
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName(moduleClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(in.getClass().getName()
                    + " should have a static inner class named Module", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("exception attempting to instantiate " + moduleClassName
                    + " for provider " + in.getName(), e);
        }
        try {
            Constructor<?> ctor = moduleClass.getDeclaredConstructor();
            // allow private or package protected ctors
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("ensure " + moduleClassName + " has a no-args constructor", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("exception attempting to instantiate " + moduleClassName
                    + " for provider " + in.getName(), e);
        }
    }

    /**
     * Creates a new manager for a provider, based on key look up from
     * {@link #listProviders()}.
     * 
     * Ex. {@code mock}
     * 
     * <pre>
     * route53 = Denominator.create(&quot;route53&quot;, credentials(accesskey, secretkey));
     * </pre>
     * 
     * @see Provider#getName()
     * @see CredentialsConfiguration
     * @throws IllegalArgumentException
     *             if the providerName is not configured, or its corresponding
     *             {@link Provider} is misconfigured or expects credentials.
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
