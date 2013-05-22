package denominator;

import com.google.common.annotations.Beta;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Multimap;
import com.google.common.io.Closer;

import dagger.Provides;

/**
 * components that all providers of {@link DNSApi} must expose.
 */
@Beta
public interface Provider {

    /**
     * configuration key associated with this {@link DNSApi}. For example,
     * {@code hopper}.
     */
    String getName();

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
     * {@link CaseFormat#LOWER_CAMEL lowerCamel} case.
     * 
     * 
     * <h3>Implementation Expectations</h3>
     * 
     * The implementing provider should throw an
     * {@link IllegalArgumentException} if it is ever supplied with an incorrect
     * count of credential parts corresponding to this. The preferred mechanism
     * to access credentials in a way that validates parameters is to use
     * {@code Provider<Credentials>} bound by
     * {@link CredentialsConfiguration}.
     * 
     * @return credential types to the labels of each part required. An empty
     *         multimap suggests the provider doesn't authenticate.
     */
    Multimap<String, String> getCredentialTypeToParameterNames();
    
    /**
     * provides components that implement the {@link DNSApi}.
     * 
     * implement this, and annotate with the following:
     * 
     * {@code  @dagger.Module(injects = DNSApiManager.class) }
     * 
     * make sure your subclass has {@link Provides} methods for the current
     * {@code Provider} (used for toString), {@link DNSApi} and {@link Closer}
     */
    Object module();
}