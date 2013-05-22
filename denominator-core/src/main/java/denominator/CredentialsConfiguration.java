package denominator;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Provider;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;

import dagger.Module;
import dagger.Provides;
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
 * Provider&lt;Credentials&gt; converter = new Provider&lt;Credentials&gt;() {
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
     * used to set a base case where no credentials are available or needed.
     */
    public static Object anonymous() {
        return credentials(AnonymousCredentials.INSTANCE);
    };

    /**
     * 
     * @param firstPart
     *            first part of credentials, such as a username or accessKey
     * @param secondPart
     *            second part of credentials, such as a password or secretKey
     */
    public static Object credentials(Object firstPart, Object secondPart) {
        return credentials(ListCredentials.from(firstPart, secondPart));
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
    public static Object credentials(Object firstPart, Object secondPart, Object thirdPart) {
        return credentials(ListCredentials.from(firstPart, secondPart, thirdPart));
    }

    /**
     * @param credentials
     *            will always be used on the provider
     */
    public static Object credentials(Credentials credentials) {
        return new ConstantCredentialsProvider(credentials);
    }

    @Module(complete = false, 
            // only used for dns services that authenticate
            library = true,
            // override any built-in credentials
            overrides = true)
    static final class ConstantCredentialsProvider {
        private final Credentials creds;

        private ConstantCredentialsProvider(Credentials creds) {
            this.creds = checkNotNull(creds, "creds");
        }

        @Provides
        public Credentials get(denominator.Provider provider) {
            return checkValidForProvider(creds, provider);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ConstantCredentialsProvider) {
                ConstantCredentialsProvider that = ConstantCredentialsProvider.class.cast(obj);
                return this.creds.equals(that.creds);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return creds.hashCode();
        }

        @Override
        public String toString() {
            return "ConstantCredentialsProvider(" + creds + ")";
        }
    }

    /**
     * @deprecated use {@link CredentialsConfiguration#credentials(Provider)}
     *             instead.
     */
    @Deprecated
    public static DynamicCredentialsProvider credentials(final Supplier<Credentials> credentials) {
        return new DynamicCredentialsProvider(new Provider<Credentials>() {
            @Override
            public Credentials get() {
                return credentials.get();
            }
        });
    }

    /**
     * @param credentials
     *            {@link Provider#get()} will be called each time credentials
     *            are needed, facilitating runtime credential changes.
     */
    public static DynamicCredentialsProvider credentials(Provider<Credentials> credentials) {
        return new DynamicCredentialsProvider(credentials);
    }

    @Module(complete = false, 
            // only used for dns services that authenticate
            library = true,
            // override any built-in credentials
            overrides = true)
    static final class DynamicCredentialsProvider {
        private final Provider<Credentials> creds;

        private DynamicCredentialsProvider(Provider<Credentials> creds) {
            this.creds = checkNotNull(creds, "creds");
        }

        @Provides
        public Credentials get(denominator.Provider provider) {
            return checkValidForProvider(creds.get(), provider);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DynamicCredentialsProvider) {
                DynamicCredentialsProvider that = DynamicCredentialsProvider.class.cast(obj);
                return this.creds.equals(that.creds);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return creds.hashCode();
        }

        @Override
        public String toString() {
            return "DynamicCredentialsProvider(" + creds + ")";
        }
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
    public static Credentials checkValidForProvider(Credentials creds, denominator.Provider provider) {
        checkNotNull(provider, "provider cannot be null");
        if (isAnonymous(creds) && provider.getCredentialTypeToParameterNames().isEmpty()) {
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

    private static boolean credentialConfigurationHasPartCount(denominator.Provider provider, int size) {
        for (String credentialType : provider.getCredentialTypeToParameterNames().keySet())
            if (provider.getCredentialTypeToParameterNames().get(credentialType).size() == size)
                return true;
        return false;
    }

    private static boolean credentialConfigurationHasKeys(denominator.Provider provider, Set<?> keys) {
        for (String credentialType : provider.getCredentialTypeToParameterNames().keySet())
            if (keys.containsAll(provider.getCredentialTypeToParameterNames().get(credentialType)))
                return true;
        return false;
    }

    /**
     * Use this method to generate a consistent error message when the
     * credentials supplied are not valid for the provider. Typically, this will
     * be the message of an {@code IllegalArgumentException}
     * 
     * @param input
     *            nullable credentials you know are invalid
     * @param provider
     *            provider they are invalid for
     */
    @Beta
    public static String exceptionMessage(Credentials input, denominator.Provider provider) {
        StringBuilder msg = new StringBuilder();
        if (input == null || input == AnonymousCredentials.INSTANCE)
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
}