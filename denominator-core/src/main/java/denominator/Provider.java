package denominator;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.google.common.annotations.Beta;
import com.google.common.base.CaseFormat;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.Closer;

import dagger.Module;
import dagger.Provides;

/**
 * provides components that implement the {@link DNSApi}.
 * 
 * subclass this, and annotate with the following:
 * 
 * {@code  @Module(entryPoints = DNSApiManager.class) }
 * 
 * make sure your subclass has {@link Provides} methods for {@code String} (used
 * for toString), {@link DNSApi} and {@link Closer}
 */
@Beta
public abstract class Provider {

    // protected to ensure subclassed
    protected Provider() {
        checkProvideThis();
        checkModuleAnnotation();
        checkLowerCamel(getCredentialTypeToParameterNames());
    }

    private void checkProvideThis() {
        Method provideThis;
        try {
            provideThis = getClass().getDeclaredMethod("provideThis");
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        } catch (SecurityException e) {
            throw Throwables.propagate(e);
        }
        checkArgument(provideThis.isAnnotationPresent(Provides.class), "add @Provides to %s()", provideThis.getName());
    }

    private void checkModuleAnnotation() {
        checkArgument(
                getClass().isAnnotationPresent(Module.class)
                        && ImmutableSet.copyOf(getClass().getAnnotation(Module.class).entryPoints()).contains(
                                DNSApiManager.class), "add @Module(entryPoints = DNSApiManager.class) to %s",
                getClass().getName());
    }

    /**
     * if we choose to support numbers, this will need to be updated
     */
    private static Pattern lowerCamel = Pattern.compile("^[a-z]+([A-Z][a-z]+)*$");

    private void checkLowerCamel(Multimap<String, String> credentialTypeToParameterNames) {
        for (Entry<String, String> entry : credentialTypeToParameterNames.entries()) {
            checkArgument(lowerCamel.matcher(entry.getKey()).matches(),
                    "please correct credential type %s to lowerCamel case", entry.getKey());
            checkArgument(lowerCamel.matcher(entry.getValue()).matches(),
                    "please correct %s credential parameter %s to lowerCamel case", entry.getKey(), entry.getValue());
        }
    }

    /**
     * configuration key associated with this {@link DNSApi}. For example,
     * {@code hopper}.
     */
    public String getName() {
        return getClass().getSimpleName().toLowerCase().replace("provider", "");
    }

    /**
     * binds the current provider, which is useful for injecting its name or
     * {@link #getCredentialTypeToParameterNames}.
     * 
     * <h4>to implement</h4>
     * 
     * <pre>
     * &#064;Provides
     * protected Provider provideThis() {
     *     return this;
     * }
     * </pre>
     */
    @Beta
    // TODO: see if dagger will support Provides for abstract classes to remove
    // boilerplate.
    protected abstract Provider provideThis();

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
     * for throwing exceptions is to employ
     * {@link CredentialsConfiguration#firstValidCredentialsForProvider(java.util.Set, Provider)}
     * . Providers who do not require credentials needn't throw an exception if
     * supplied with them.
     * 
     * @return credential types to the labels of each part required. An empty
     *         multimap suggests the provider doesn't authenticate.
     */
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.of();
    }
    
    /**
     * present when there is a provider-specific means to supply credentials,
     * such as {@code IAM Instance Profile}
     */
    public Optional<Supplier<Credentials>> defaultCredentialSupplier() {
        return Optional.absent();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        return equal(this.getName(), Provider.class.cast(obj).getName());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", getName()).toString();
    }
}