package denominator;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.ofInstance;
import static com.google.common.collect.Iterables.getOnlyElement;
import static dagger.Provides.Type.SET;
import static denominator.Credentials.ListCredentials.asList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dagger.Module;
import dagger.Provides;
import dagger.Provides.Type;
import denominator.Credentials.AnonymousCredentials;
import denominator.Credentials.ListCredentials;

/**
 * use this for providers who need credentials.
 * 
 * ex. for two-part
 * 
 * <pre>
 * ultra = Denominator.create(new UltraDNSProvider(), credentials(username, password));
 * route53 = Denominator.create(new Route53Provider(), credentials(accesskey, secretkey));
 * </pre>
 * 
 * ex. for three-part
 * 
 * <pre>
 * dynect = Denominator.create(new DynECTProvider(), credentials(customer, username, password));
 * </pre>
 * 
 * ex. for dynamic credentials
 * 
 * <pre>
 * final AWSCredentialsProvider provider = // from wherever
 * Supplier&lt;Credentials&gt; converter = new Supplier&lt;Credentials&gt;() {
 *     public Credentials get() {
 *         AWSCredentials awsCreds = provider.getCredentials();
 *         return credentials(awsCreds.getAWSAccessKeyId(), awsCreds.getAWSSecretKey());
 *     }
 * };
 * 
 * route53 = Denominator.create(new Route53Provider(), credentials(converter));
 * </pre>
 */
public class CredentialsConfiguration {

    private CredentialsConfiguration() {
    }

    /**
     * Credential suppliers are {@link Type#SET} binding, and order matters. It
     * is important to track the types of exceptions possible when calling
     * {@link Supplier#get()}, as these could otherwise surprise users. 
     */
    @Module(injects = DNSApiManager.class, complete = false, library = true)
    static class CredentialsSupplier {
        private final Supplier<Credentials> creds;

        private CredentialsSupplier(Supplier<Credentials> creds) {
            this.creds = creds;
        }

        @Provides(type = SET)
        @Singleton
        Supplier<Credentials> supplyCredentials() {
            return creds;
        }
    }

    /**
     * used to set a base case where no credentials are available or needed.
     */
    static CredentialsSupplier anonymous() {
        return new CredentialsSupplier(Suppliers.<Credentials> ofInstance(AnonymousCredentials.INSTANCE));
    };

    /**
     * 
     * @param firstPart
     *            first part of credentials, such as a username or accessKey
     * @param secondPart
     *            second part of credentials, such as a password or secretKey
     */
    public static CredentialsSupplier credentials(Object firstPart, Object secondPart) {
        return new CredentialsSupplier(ofInstance(ListCredentials.from(firstPart, secondPart)));
    }

    /**
     * 
     * @param firstPart
     *            first part of credentials, such as a customer or tenant
     * @param secondPart
     *            second part of credentials, such as a username or accessKey
     * @param thirdPart
     *            third part of credentials, such as a password or secretKey
     */
    public static CredentialsSupplier credentials(Object firstPart, Object secondPart, Object thirdPart) {
        return new CredentialsSupplier(ofInstance(ListCredentials.from(firstPart, secondPart, thirdPart)));
    }

    /**
     * @param credentials
     *            will always be used on the provider
     */
    public static CredentialsSupplier credentials(Credentials credentials) {
        return new CredentialsSupplier(ofInstance(credentials));
    }

    /**
     * @param credentials
     *            {@link Supplier#get()} will be called each time credentials
     *            are needed, facilitating runtime credential changes.
     */
    public static CredentialsSupplier credentials(Supplier<Credentials> credentials) {
        return new CredentialsSupplier(credentials);
    }

    /**
     * returns the first configured credential for the provider from any
     * supplier, falling back to {@link AnonymousCredentials} if supported.
     * 
     * @throws IllegalArgumentException
     *             if one of the credentials returned is invalid for this
     *             provider.
     * @see CredentialsConfiguration#checkValidForProvider(Credentials,
     *      Provider)
     */
    public static Credentials firstValidCredentialsForProvider(Set<Supplier<Credentials>> allSuppliers,
            Provider provider) {
        for (Supplier<Credentials> supplier : allSuppliers) {
            Credentials credentials = supplier.get();
            return checkValidForProvider(credentials, provider);
        }
        return checkValidForProvider(AnonymousCredentials.INSTANCE, provider);
    }

    /**
     * checks that the supplied input is valid, or throws an
     * {@code IllegalArgumentException} if not. Users of this are guaranteed
     * that the {@code input} matches the count of parameters of a credential
     * type listed in {@link Provider#getCredentialTypeToParameterNames()}.
     * 
     * <h4>Coercion to {@code AnonymousCredentials}</h4>
     * 
     * if {@link Provider#getCredentialTypeToParameterNames()} is empty, then no
     * credentials are required. When this is true, the following cases will
     * return {@code AnonymousCredentials}.
     * <ul>
     * <li>when {@code input} is null</li>
     * <li>when {@code input} is an instance of {@code AnonymousCredentials}</li>
     * <li>when {@code input} is an empty instance of {@code Map} or
     * {@code List}</li>
     * </ul>
     * 
     * <h4>Validation Rules</h4>
     * 
     * See {@link Credentials} for Validation Rules
     * 
     * @param creds
     *            nullable credentials to test
     * @param provider
     *            context which helps create a useful error message on failure.
     * @throws IllegalArgumentException
     *             if provider requires a different amount of credential parts
     *             than {@code input}
     * @return correct Credentials value which can be
     *         {@link AnonymousCredentials} if {@code input} was null and
     *         credentials are not needed.
     */
    public static Credentials checkValidForProvider(Credentials creds, Provider provider) {
        checkNotNull(provider, "provider cannot be null");
        if (isAnonymous(creds)) {
            checkArgument(provider.getCredentialTypeToParameterNames().isEmpty(), exceptionMessage(null, provider));
            return AnonymousCredentials.INSTANCE;
        } else if (creds instanceof Map) {
            // check Map first as clojure Map implements List Map.Entry
            if (credentialConfigurationHasKeys(provider, Map.class.cast(creds).keySet()))
                return creds;
        } else if (creds instanceof List) {
            if (credentialConfigurationHasPartCount(provider, List.class.cast(creds).size()))
                return creds;
        }
        throw new IllegalArgumentException(exceptionMessage(creds, provider));
    }

    private final static boolean isAnonymous(Credentials input) {
        if (input == null)
            return true;
        if (input instanceof AnonymousCredentials)
            return true;
        if (input instanceof Map)
            return Map.class.cast(input).isEmpty();
        if (input instanceof List)
            return List.class.cast(input).isEmpty();
        return false;
    }

    private static boolean credentialConfigurationHasPartCount(Provider provider, int size) {
        for (String credentialType : provider.getCredentialTypeToParameterNames().keySet())
            if (provider.getCredentialTypeToParameterNames().get(credentialType).size() == size)
                return true;
        return false;
    }

    private static boolean credentialConfigurationHasKeys(Provider provider, Set<?> keys) {
        for (String credentialType : provider.getCredentialTypeToParameterNames().keySet())
            if (keys.containsAll(provider.getCredentialTypeToParameterNames().get(credentialType)))
                return true;
        return false;
    }

    private static String exceptionMessage(Credentials input, Provider provider) {
        StringBuilder msg = new StringBuilder();
        if (input == null)
            msg.append("no credentials supplied. ");
        else
            msg.append("incorrect credentials supplied. ");
        msg.append(provider.getName()).append(" requires ");

        Map<String, Collection<String>> credentialTypeToParameterNames = provider.getCredentialTypeToParameterNames()
                .asMap();
        if (credentialTypeToParameterNames.size() == 1) {
            msg.append(on(", ").join(getOnlyElement(credentialTypeToParameterNames.values())));
        } else {
            msg.append("one of the following forms: when type is ");
            for (Entry<String, Collection<String>> entry : credentialTypeToParameterNames.entrySet()) {
                msg.append(entry.getKey()).append(": ").append(on(", ").join(entry.getValue())).append("; ");
            }
            msg.trimToSize();
            msg.setLength(msg.length() - 2);// remove last '; '
        }
        return msg.toString();
    }

    /**
     * Credentials are coerced from
     * {@link CredentialsConfiguration#firstValidCredentialsForProvider}, which
     * can throw an {@code IllegalArgumentException}.
     */
    public static class CredentialsAsList implements Supplier<List<Object>> {

        private final Provider provider;
        private final Set<Supplier<Credentials>> sources;

        // package private as we cannot inject private ctors in dagger, yet.
        @Inject
        CredentialsAsList(Provider provider, Set<Supplier<Credentials>> sources) {
            this.provider = provider;
            this.sources = sources;
        }

        @Override
        public List<Object> get() {
            return asList(firstValidCredentialsForProvider(sources, provider));
        }
    }
}