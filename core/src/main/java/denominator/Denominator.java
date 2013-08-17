package denominator;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * Entry-point to create instances of {@link DNSApiManager}
 * 
 * ex.
 * 
 * <pre>
 * import static denominator.CredentialsConfiguration.credentials;
 * 
 * ...
 * 
 * ultraDns = Denominator.create(new UltraDNSProvider(), credentials(username, password));
 * </pre>
 * 
 * <br>
 * <br>
 * <b>Alternative</b><br>
 * 
 * This class implies use of reflection to lookup the dagger module associated
 * with the provider. You can alternatively not use this class and instead use
 * dagger directly.
 * 
 * ex.
 * 
 * <pre>
 * import static denominator.CredentialsConfiguration.credentials;
 * import static denominator.Denominator.provider;
 * 
 * ...
 * 
 * ultraDns = ObjectGraph.create(provider(new UltraDNSProvider()),
 *                               new UltraDNSProvider.Module(),
 *                               credentials(username, password)).get(DNSApiManager.class);
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
     * <br>
     * <br>
     * <b>Performance Note</b><br>
     * 
     * The implicit call {@link ServiceLoader#load(Class)} can add delays
     * measurable in 10s to hundreds of milliseconds depending on the number of
     * jars in your classpath. If you desire speed, it is best to instantiate
     * Providers directly.
     */
    public static Iterable<Provider> providers() {
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
     * @see #providers
     * @throws IllegalArgumentException
     *             if the input provider is misconfigured or expects
     *             credentials.
     */
    public static DNSApiManager create(Provider in, Object... modules) {
        Object[] modulesForGraph = modulesForGraph(in, modules).toArray();
        try {
            return ObjectGraph.create(modulesForGraph).get(DNSApiManager.class);
        } catch (IllegalStateException e) {
            // much simpler to special-case when a credential module is needed,
            // but not supplied, than do too much magic.
            if (e.getMessage().contains("No binding for denominator.Credentials")
                    && !in.credentialTypeToParameterNames().isEmpty()) {
                throw new IllegalArgumentException(CredentialsConfiguration.exceptionMessage(null, in));
            }
            throw e;
        }
    }

    private static List<Object> modulesForGraph(Provider in, Object... modules) {
        List<Object> modulesForGraph = new ArrayList<Object>(3);
        modulesForGraph.add(provider(in));
        modulesForGraph.add(instantiateModule(in));
        if (modules != null)
            for (Object module : modules)
                modulesForGraph.add(module);
        return modulesForGraph;
    }

    private static Object instantiateModule(Provider in) throws IllegalArgumentException {
        String moduleClassName;
        if (in.getClass().isAnonymousClass()) {
            moduleClassName = in.getClass().getSuperclass().getName() + "$Module";
        } else {
            moduleClassName = in.getClass().getName() + "$Module";
        }
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName(moduleClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(in.getClass().getName()
                    + " should have a static inner class named Module", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("exception attempting to instantiate " + moduleClassName
                    + " for provider " + in.name(), e);
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
                    + " for provider " + in.name(), e);
        }
    }

    /**
     * Creates a new manager for a provider, based on key look up from
     * {@link #providers()}.
     * 
     * Ex. {@code mock}
     * 
     * <pre>
     * route53 = Denominator.create(&quot;route53&quot;, credentials(accesskey, secretkey));
     * </pre>
     * 
     * @see Provider#name()
     * @see CredentialsConfiguration
     * @throws IllegalArgumentException
     *             if the providerName is not configured, or its corresponding
     *             {@link Provider} is misconfigured or expects credentials.
     */
    public static DNSApiManager create(String providerName, Object... modules) throws IllegalArgumentException {
        checkNotNull(providerName, "providerName");
        Provider matchedProvider = null;
        List<String> providerNames = new ArrayList<String>();
        for (Provider provider : providers()) {
            if (provider.name().equals(providerName)) {
                matchedProvider = provider;
                break;
            }
            providerNames.add(provider.name());
        }
        checkArgument(matchedProvider != null, "provider %s not in set of configured providers: %s", providerName,
                providerNames);
        return create(matchedProvider, modules);
    }

    /**
     * Use this when building {@link DNSApiManager} via Dagger.
     * 
     * ex. when no runtime changes to the provider are necessary:
     * 
     * <pre>
     * ultraDns = ObjectGraph.create(provider(new UltraDNSProvider()),
     *                               new UltraDNSProvider.Module(),
     *                               credentials(username, password)).get(DNSApiManager.class);
     * </pre>
     * 
     * ex. for dynamic provider
     * 
     * <pre>
     * Provider fromDiscovery = new UltraDNSProvider() {
     *     public String getUrl() {
     *         return discovery.getUrlFor(&quot;ultradns&quot;);
     *     }
     * };
     * 
     * ultraDns = ObjectGraph.create(provider(fromDiscovery),
     *                               new UltraDNSProvider.Module(),
     *                               credentials(username, password)).get(DNSApiManager.class);
     * </pre>
     */
    public static Object provider(denominator.Provider provider) {
        return new ProvideProvider(provider);
    }

    @Module(injects = DNSApiManager.class, complete = false)
    static final class ProvideProvider implements javax.inject.Provider<Provider> {
        private final denominator.Provider provider;

        private ProvideProvider(denominator.Provider provider) {
            this.provider = checkNotNull(provider, "provider");
        }

        @Provides
        @Singleton
        public Provider get() {
            return provider;
        }

        @Override
        public String toString() {
            return "Provides(" + provider + ")";
        }
    }
}
