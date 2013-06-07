package denominator;

import javax.inject.Singleton;

import com.google.common.annotations.Beta;
import com.google.common.collect.Multimap;

import denominator.model.Zone;

/**
 * Metadata about a provider of DNS services.
 * 
 * <h3>Writing a Provider</h3>
 * 
 * The current implementation of {@link Denominator#create(Provider, Object...)}
 * expects all {@code Provider} implementations to expose a static inner class
 * named {@code Module}.
 * <p/>
 * The inner class is expected to have a {@link dagger.Module} annotation such
 * as below:
 * 
 * <pre>
 * public class MockProvider extends BasicProvider {
 * 
 *     &#064;dagger.Module(injects = DNSApiManager.class, complete = false, includes = NothingToClose.class)
 *     public static final class Module {
 * </pre>
 * 
 * Look at {@link denominator.mock.MockProvider.Module} for an example of a
 * valid provider module.
 * 
 * <h3>Expected Use</h3>
 * Provider instances are bound in {@link javax.inject.Singleton} scope.
 * However, results of all methods are permitted to change at runtime. For
 * example, a change to the value returned by {@link #getUrl()} should affect
 * the remote connection to the DNS provider.
 */
@Beta
@Singleton
public interface Provider {
    /**
     * @deprecated Will be removed in denominator 2.0. Please use {@link #name}
     */
    @Deprecated
    String getName();

    /**
     * configuration key associated with this {@link DNSApi}. For example,
     * {@code hopper}.
     */
    String name();

    /**
     * @deprecated Will be removed in denominator 2.0. Please use {@link #url}
     */
    @Deprecated
    String getUrl();

    /**
     * The base API URL of the DNS Provider. Typically, a http url, such as
     * {@code https://api/v2}. For in-memory providers, we expect the scheme to
     * be {@code mem}. For example, {@code mem://mock}. Encoding credentials in
     * the URL is neither expected nor supported.
     */
    String url();

    /**
     * Certain providers support multiple zones with the same
     * {@link Zone#name name}. These are served by different name servers and
     * used to provide different internal vs external views, environment testing
     * or smoother zone transfers for the same name.
     * 
     * @return true when {@link Zone#id() zone id} is present and
     *         {@link Zone#idOrName()} returns the {@link Zone#id() zone id}.
     *         If false, {@link Zone#idOrName()} will returns
     *         {@link Zone#name zone name}.
     */
    boolean supportsDuplicateZoneNames();

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #credentialTypeToParameterNames}
     */
    @Deprecated
    Multimap<String, String> getCredentialTypeToParameterNames();

    /**
     * Description of the credential parameters needed for this provider by
     * type. Multiple entries are present when the provider supports multiple
     * credential types, and the order of these entries suggests priority. No
     * entries suggest this provider does not require credentials. Values are an
     * ordered list of labels that describe the credential parts required to
     * authenticate, for example {@code ["username", "password"]}.
     * 
     * <h3>Credential Type</h3>
     * 
     * The credential type keys intend to easily disambiguate cases where
     * multiple means of authentication exist, or where authentication means
     * share the same number of parts. For example, in Amazon, the keySet might
     * be {@code ["accessKey", "stsSession"]}, where default is contains the
     * access and secret key, and {@code stsSession} adds a token. In an
     * {@code OpenStack} provider the keySet might be
     * {@code ["password", "apiAccessKey"]}, which disambiguates the case where
     * both {@code password} and {@code apiAccessKey} authentication require the
     * same number of parts.
     * 
     * <h3>Credential Labels</h3>
     * 
     * Values are an ordered list of labels that describe the credential parts
     * required to authenticate, for example {@code ["username", "password"]}.
     * This information is targeted at users who may otherwise be confused what
     * secrets are necessary for this provider.
     * 
     * <h4>Example</h4>
     * 
     * Given an entry with values: {@code ["accessKey", "secretKey"]}, we know
     * the order of the parameters in a {@code Iterable}-based Credentials
     * object, or the keys needed for a {@code Map}-based one.
     * 
     * <pre>
     * return new BasicAWSCredentials(creds.get(0).toString(), creds.get(1).toString());
     * // or if map-backed
     * return new BasicAWSCredentials(creds.get(&quot;accessKey&quot;).toString(), creds.get(&quot;secretKey&quot;).toString());
     * </pre>
     * 
     * <h3>Formatting</h3>
     * 
     * Both the keys and the values of this {@link Multimap Multimap} are in
     * {@link com.google.common.base.CaseFormat#LOWER_CAMEL lowerCamel} case.
     * 
     * 
     * <h3>Implementation Expectations</h3>
     * 
     * The implementing provider should throw an
     * {@link IllegalArgumentException} if it is ever supplied with an incorrect
     * count of credential parts corresponding to this. The preferred mechanism
     * to access credentials in a way that validates parameters is to use
     * {@code Provider<Credentials>} bound by {@link CredentialsConfiguration}.
     * 
     * @return credential types to the labels of each part required. An empty
     *         multimap suggests the provider doesn't authenticate.
     */
    Multimap<String, String> credentialTypeToParameterNames();
}
