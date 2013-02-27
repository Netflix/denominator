package denominator;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Abstractly encapsulates credentials used by a Provider. Descriptions of what
 * type of Credentials a Provider can use are found via
 * {@link Provider#getCredentialTypeToParameterNames()}.
 * 
 * <h4>All Credentials are Anonymous, implement Map or List</h4>
 * 
 * You must either subclass roots listed here, or ensure your implementation
 * classes implement {@code Map<String, ?>} or {@code List<?>}.
 * 
 * <h4>Validation Rules</h4>
 * <ul>
 * <li>{@link AnonymousCredentials}, null, or empty credentials are permitted
 * when {@link Provider#getCredentialTypeToParameterNames()} is empty</li>
 * <li>{@link ListCredentials} or {@code Credentials} which implement
 * {@code List} are permitted when its {@code size} match the size of an entry
 * in {@link Provider#getCredentialTypeToParameterNames()}</li>
 * <li>{@link MapCredentials} or {@code Credentials} which implement {@code Map}
 * are permitted when its keys match the values of an entry in
 * {@link Provider#getCredentialTypeToParameterNames()}</li>
 * </ul>
 * 
 * <h4>Parameters</h4>
 * 
 * While most credential parameters are Strings, and most providers will be able
 * to coerce parameters from Strings, if you choose to create your own
 * Credential implementations, allow users to pass {@code Object} parameters of
 * the correct type where possible.
 * <p/>
 * For example, certain providers may prefer a {@code Certificate} or
 * {@code PrivateKey} parameter vs a relatively expensive coercion from a
 * PEM-encoded String.
 * 
 */
public interface Credentials {
    /**
     * For use in providers that do not authenticate, or those that permit
     * anonymous calls.
     */
    public static enum AnonymousCredentials implements Credentials {
        INSTANCE;
    }

    /**
     * Credentials where the position and count of parameters is enough to
     * differentiate one type of credentials from another.
     * 
     * <h4>Example</h4>
     * 
     * In {@code route53}, one can use normal {@code accessKey} credentials, or
     * temporary ones from {code STS}. In this case, you can get by simply by
     * differentiating on count of parameters.
     * 
     * <p/>
     * Normally, the caller passes:
     * 
     * <pre>
     * ["AAFF12AA", "BB34FF"]
     * </pre>
     * 
     * When using STS, the caller passes an additional parameter corresponding
     * to the session token:
     * 
     * <pre>
     * ["AAFF12AA", "BB34FF", "FFFeeeEE"]
     * </pre>
     * 
     * @see Provider#getCredentialTypeToParameterNames()
     */
    public static abstract class ListCredentials extends ForwardingList<Object> implements Credentials {

        /**
         * returns a {@code Credentials} view of the input
         * {@code parts}.
         * 
         * @param parts
         *            corresponds directly to the credentials needed by a
         *            provider
         * @return credentials view of {@code parts} or
         *         {@link AnonymousCredentials} if empty
         */
        public static Credentials from(final List<?> parts) {
            if (parts == null || parts.isEmpty())
                return AnonymousCredentials.INSTANCE;
            return new ListCredentials() {
                @SuppressWarnings("unchecked")
                protected List<Object> delegate() {
                    return List.class.cast(parts);
                }
            };
        }

        /**
         * see {@link #from(List)}
         */
        public static Credentials from(Object... parts) {
            return from(ImmutableList.copyOf(checkNotNull(parts, "credentials")));
        }

        /**
         * Make it very easy on providers who only accept a single credentials
         * type. The following will coerce anything that implements {@code Map}
         * or {@code List} to {@code ListCredentials}. It is suggested to use
         * {@link CredentialsConfiguration#firstValidCredentialsForProvider(java.util.Set, Provider)}
         * prior to calling this, as otherwise the resulting credentials may not
         * be valid for the provider.
         * 
         * <h4>Example</h4> The following example is how this could be used from
         * within a method in {@link Provider} to simplify credential conversion
         * regardless of input type.
         * 
         * <pre>
         * Credentials validatedInput = CredentialsConfiguration.firstValidCredentialsForProvider(sources, this);
         * List&lt;Object&gt; creds = ListCredentials.asList(validatedInput);
         * return new BasicAWSCredentials(creds.get(0).toString(), creds.get(1).toString());
         * </pre>
         * 
         * @return copy of the credentials values
         * @throws IllegalArgumentException
         *             if input is not a {@code Map} or {@code List}, or it is
         *             empty.
         *             
         */
        public static List<Object> asList(Credentials in) {
            checkNotNull(in, "credentials");
            if (in instanceof ListCredentials) {
                return ListCredentials.class.cast(in);
            } else if (in instanceof Map || in instanceof List) {
                Iterable<?> values = (in instanceof Map) ? Map.class.cast(in).values() : List.class.cast(in);
                if (Iterables.isEmpty(values))
                    throw new IllegalArgumentException("cannot convert empty credentials to List<Object>");
                return ImmutableList.copyOf(values);
            }
            throw new IllegalArgumentException("cannot convert " + in.getClass() + " to ListCredentials");
        }

        protected ListCredentials() {
        }
    }

    /**
     * Credentials in the form of named parameters, useful when a provider
     * accepts two different credential types with the same count of parameters.
     * 
     * <h4>Example</h4>
     * 
     * In OpenStack, both {@code accessKey} and {@code password} credentials
     * require two parts. In this case, {@code MapCredentials} can name the
     * parts to differentiate them.
     * 
     * <p/>
     * For example, when using {@code password}, the caller passes:
     * 
     * <pre>
     * {"username": "foo", "password": "bar"}
     * </pre>
     * 
     * Whereas for {@code accessKey}, the caller passes:
     * 
     * <pre>
     * {"accessKey": "AAFF12AA", "secretKey": "BB34FF"}
     * </pre>
     * 
     * @see Provider#getCredentialTypeToParameterNames()
     */
    public static abstract class MapCredentials extends ForwardingMap<String, Object> implements Credentials {

        /**
         * returns a {@code Credentials} view of the input {@code kwargs}
         * 
         * @param kwargs
         *            corresponds directly to the credentials needed by a
         *            provider
         * @return credentials view of {@code kwargs} or
         *         {@link AnonymousCredentials} if null or empty
         */
        public static Credentials from(final Map<String, ?> kwargs) {
            if (kwargs == null ||kwargs.isEmpty())
                return AnonymousCredentials.INSTANCE;
            return new MapCredentials() {
                @SuppressWarnings("unchecked")
                protected Map<String, Object> delegate() {
                    return Map.class.cast(kwargs);
                }
            };
        }

        protected MapCredentials() {
        }
    }
}