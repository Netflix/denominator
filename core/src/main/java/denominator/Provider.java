package denominator;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import denominator.model.ResourceRecordSet;

/**
 * Metadata about a provider of DNS services.
 *
 * <br> <br> <b>Writing a Provider</b><br>
 *
 * The current implementation of {@link Denominator#create(Provider, Object...)} expects all {@code
 * Provider} implementations to expose a static inner class named {@code Module}. <br> The inner
 * class is expected to have a {@link dagger.Module} annotation such as below:
 *
 * <pre>
 * public class MockProvider extends BasicProvider {
 *
 *     &#064;dagger.Module(injects = DNSApiManager.class, complete = false, includes =
 * NothingToClose.class)
 *     public static final class Module {
 * </pre>
 *
 * Look at {@link denominator.mock.MockProvider.Module} for an example of a valid provider module.
 *
 * <br> <br> <b>Expected Use</b><br> Provider instances are bound in {@link javax.inject.Singleton}
 * scope. However, results of all methods are permitted to change at runtime. For example, a change
 * to the value returned by {@link #url()} should affect the remote connection to the DNS provider.
 */
public interface Provider {

  /**
   * configuration key associated with this {@link DNSApi}. For example, {@code hopper}.
   */
  String name();

  /**
   * The base API URL of the DNS Provider. Typically, a http url, such as {@code https://api/v2}.
   * For in-memory providers, we expect the scheme to be {@code mem}. For example, {@code
   * mem://mock}. Encoding credentials in the URL is neither expected nor supported.
   */
  String url();

  /**
   * The set of basic {@link ResourceRecordSet#type() record types} that are supported by {@link
   * ResourceRecordSetApi} commands. <br> For example:
   *
   * <pre>
   * ["A", "AAAA", "CNAME", "HINFO", "MX", "PTR", "RP", "SRV", "TXT", "NAPTR"]
   * </pre>
   */
  Set<String> basicRecordTypes();

  /**
   * Maps a profile value to a collection of supported record types. If empty, the provider does not
   * support advanced records.
   *
   * <p/> For example:
   *
   * <pre>
   * { "geo" : ["A", "AAAA", "CNAME", "HINFO", "MX", "PTR", "RP", "SRV", "TXT", "NAPTR"],
   *   "weighted" : ["A", "AAAA", "CNAME"] }
   * </pre>
   *
   * <p/> Well known record types are in the {@code denominator.model.profile} package.
   */
  Map<String, Collection<String>> profileToRecordTypes();

  /**
   * Duplicate zones can exist with the same name.
   */
  boolean supportsDuplicateZoneNames();

  /**
   * Description of the credential parameters needed for this provider by type. Multiple entries are
   * present when the provider supports multiple credential types, and the order of these entries
   * suggests priority. No entries suggest this provider does not require credentials. Values are an
   * ordered list of labels that describe the credential parts required to authenticate, for example
   * {@code ["username", "password"]}.
   *
   * <br> <br> <b>Credential Type</b><br>
   *
   * The credential type keys intend to easily disambiguate cases where multiple means of
   * authentication exist, or where authentication means share the same number of parts. For
   * example, in Amazon, the keySet might be {@code ["accessKey", "stsSession"]}, where default is
   * contains the access and secret key, and {@code stsSession} adds a token. In an {@code
   * OpenStack} provider the keySet might be {@code ["password", "apiAccessKey"]}, which
   * disambiguates the case where both {@code password} and {@code apiAccessKey} authentication
   * require the same number of parts.
   *
   * <br> <br> <b>Credential Labels</b><br>
   *
   * Values are an ordered list of labels that describe the credential parts required to
   * authenticate, for example {@code ["username", "password"]}. This information is targeted at
   * users who may otherwise be confused what secrets are necessary for this provider.
   *
   * <br> <br> <b>Example</b><br>
   *
   * Given an entry with values: {@code ["accessKey", "secretKey"]}, we know the order of the
   * parameters in a {@code Collection}-based Credentials object, or the keys needed for a {@code
   * Map}-based one.
   *
   * <pre>
   * return new BasicAWSCredentials(creds.get(0).toString(), creds.get(1).toString());
   * // or if map-backed
   * return new BasicAWSCredentials(creds.get(&quot;accessKey&quot;).toString(),
   * creds.get(&quot;secretKey&quot;).toString());
   * </pre>
   *
   * <br> <br> <b>Formatting</b><br>
   *
   * Both the keys and the values of this map are in {@code lowerCamel} case.
   *
   *
   * <br> <br> <b>Implementation Expectations</b><br>
   *
   * The implementing provider should throw an {@link IllegalArgumentException} if it is ever
   * supplied with an incorrect count of credential parts corresponding to this. The preferred
   * mechanism to access credentials in a way that validates parameters is to use {@code
   * Provider<Credentials>} bound by {@link CredentialsConfiguration}.
   *
   * @return credential types to the labels of each part required. An empty multimap suggests the
   * provider doesn't authenticate.
   */
  Map<String, Collection<String>> credentialTypeToParameterNames();
}
