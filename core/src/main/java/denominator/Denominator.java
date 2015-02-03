package denominator;

import java.util.ArrayList;
import java.util.List;

import dagger.ObjectGraph;

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
 * <br> <br> <b>Alternative</b><br>
 *
 * This class implies use of reflection to lookup the dagger module associated with the provider.
 * You can alternatively not use this class and instead use dagger directly.
 *
 * ex.
 *
 * <pre>
 * import static denominator.CredentialsConfiguration.credentials;
 * import static denominator.Providers.provide;
 *
 * ...
 *
 * ultraDns = ObjectGraph.create(provider(new UltraDNSProvider()),
 *                               new UltraDNSProvider.Module(),
 *                               credentials(username, password)).get(DNSApiManager.class);
 * </pre>
 */
public final class Denominator {

  /**
   * @deprecated use {@link Providers#list()}. to be removed in denominator 4.
   */
  @Deprecated
  public static Iterable<Provider> providers() {
    return Providers.list();
  }

  /**
   * Creates a new manager for a provider using its type, such as {@link
   * denominator.mock.MockProvider}.
   *
   * ex.
   *
   * <pre>
   * ultraDns = Denominator.create(new UltraDNSProvider(), credentials(username, password));
   * </pre>
   *
   * @throws IllegalArgumentException if the input provider is misconfigured or expects
   *                                  credentials.
   * @see CredentialsConfiguration
   * @see #providers
   */
  public static DNSApiManager create(Provider in, Object... modules) {
    Object[] modulesForGraph = modulesForGraph(in, modules).toArray();
    try {
      return ObjectGraph.create(modulesForGraph).get(DNSApiManager.class);
    } catch (IllegalStateException e) {
      // much simpler to special-case when a credential module is needed,
      // but not supplied, than do too much magic.
      if (e.getMessage().contains("denominator.Credentials could not be bound")
          && !in.credentialTypeToParameterNames().isEmpty()) {
        throw new IllegalArgumentException(CredentialsConfiguration.exceptionMessage(null, in));
      }
      throw e;
    }
  }

  private static List<Object> modulesForGraph(Provider in, Object... modules) {
    List<Object> modulesForGraph = new ArrayList<Object>(3);
    modulesForGraph.add(Providers.provide(in));
    modulesForGraph.add(Providers.instantiateModule(in));
    if (modules != null) {
      for (Object module : modules) {
        modulesForGraph.add(module);
      }
    }
    return modulesForGraph;
  }

  /**
   * Creates a new manager for a provider, based on key look up from {@link #providers()}.
   *
   * Ex. {@code mock}
   *
   * <pre>
   * route53 = Denominator.create(&quot;route53&quot;, credentials(accesskey, secretkey));
   * </pre>
   *
   * @throws IllegalArgumentException if the providerName is not configured, or its corresponding
   *                                  {@link Provider} is misconfigured or expects credentials.
   * @see Provider#name()
   * @see CredentialsConfiguration
   */
  public static DNSApiManager create(String providerName, Object... modules)
      throws IllegalArgumentException {
    Provider matchedProvider = Providers.getByName(providerName);
    return create(matchedProvider, modules);
  }

  /**
   * @deprecated use {@link Providers#provide}. to be removed in denominator 4.
   */
  public static Object provider(denominator.Provider provider) {
    return Providers.provide(provider);
  }

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
}
