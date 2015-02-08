package denominator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Utilities for working with denominator providers.
 */
public final class Providers {

  /**
   * Returns the currently configured {@link Provider providers} from {@link
   * ServiceLoader#load(Class)}.
   *
   * <br> <br> <b>Performance Note</b><br>
   *
   * The implicit call {@link ServiceLoader#load(Class)} can add delays measurable in 10s to
   * hundreds of milliseconds depending on the number of jars in your classpath. If you desire
   * speed, it is best to instantiate Providers directly.
   */
  public static Iterable<Provider> list() {
    return ServiceLoader.load(Provider.class);
  }

  /**
   * Key look up from {@link #list()}.
   *
   * @param providerName corresponds to {@link Provider#name()}. ex {@code ultradns}
   * @throws IllegalArgumentException if the providerName is not configured
   * @see #list()
   */
  public static Provider getByName(String providerName) {
    checkNotNull(providerName, "providerName");
    Provider matchedProvider = null;
    List<String> providerNames = new ArrayList<String>();
    for (Provider provider : list()) {
      if (provider.name().equals(providerName)) {
        matchedProvider = provider;
        break;
      }
      providerNames.add(provider.name());
    }
    checkArgument(matchedProvider != null, "provider %s not in set of configured providers: %s",
                  providerName,
                  providerNames);
    return matchedProvider;
  }

  /**
   * Overrides the {@link Provider#url()} of a given provider via reflectively calling its url
   * constructor.
   *
   * ex.
   *
   * <pre>
   * provider = withUrl(getByName(providerName), overrideUrl);
   * module = getByName(provider);
   * ultraDns = ObjectGraph.create(provide(provider), module, credentials(username,
   * password)).get(DNSApiManager.class);
   * </pre>
   *
   * @param url corresponds to {@link Provider#url()}. ex {@code http://apiendpoint}
   * @throws IllegalArgumentException if the there's no constructor that accepts a string argument.
   */
  public static Provider withUrl(Provider provider, String url) {
    checkNotNull(provider, "provider");
    checkNotNull(url, "url");
    try {
      Constructor<?> ctor = provider.getClass().getDeclaredConstructor(String.class);
      // allow private or package protected ctors
      ctor.setAccessible(true);
      return Provider.class.cast(ctor.newInstance(url));
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          provider.getClass() + " does not have a String parameter constructor", e);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "exception attempting to instantiate " + provider.getClass()
          + " for provider " + provider, e);
    }
  }

  /**
   * Instantiates the dagger module associated with the provider reflectively. Use this when
   * building {@link DNSApiManager} via Dagger when looking up a provider by name.
   *
   * ex.
   *
   * <pre>
   * provider = withUrl(getByName(providerName), overrideUrl);
   * module = getByName(provider);
   * ultraDns = ObjectGraph.create(provide(provider), module, credentials(username,
   * password)).get(DNSApiManager.class);
   * </pre>
   *
   * @throws IllegalArgumentException if there is no static inner class named {@code Module}, or it
   *                                  is not possible to instantiate it.
   */
  public static Object instantiateModule(Provider in) throws IllegalArgumentException {
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
      throw new IllegalArgumentException("ensure " + moduleClassName + " has a no-args constructor",
                                         e);
    } catch (Exception e) {
      throw new IllegalArgumentException("exception attempting to instantiate " + moduleClassName
                                         + " for provider " + in.name(), e);
    }
  }

  /**
   * Use this when building {@link DNSApiManager} via Dagger.
   *
   * ex. when no runtime changes to the provider are necessary:
   *
   * <pre>
   * ultraDns = ObjectGraph.create(provide(new UltraDNSProvider()), new UltraDNSProvider.Module(),
   *         credentials(username, password)).get(DNSApiManager.class);
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
   * ultraDns = ObjectGraph.create(provide(fromDiscovery), new UltraDNSProvider.Module(),
   * credentials(username, password))
   *         .get(DNSApiManager.class);
   * </pre>
   */
  public static Object provide(denominator.Provider provider) {
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
