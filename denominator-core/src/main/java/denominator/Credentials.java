package denominator;

import static com.google.common.base.Objects.equal;

import com.google.common.base.Objects;

/**
 * Abstractly encapsulates credentials used by a Provider. We only support
 * providers that have a maximum of 3 part credentials.
 */
public abstract class Credentials {

    /**
     * convenience method that creates an instance of two-part credentials.
     * 
     * @param firstPart
     *            first part of the credentials, for example your username.
     * @param secondPart
     *            second part of the credentials, for example your password.
     */
    public static <F, S> Credentials twoPartCredentials(final F firstPart, final S secondPart) {
        return new TwoPartCredentials<F, S>() {
            public F getFirstPart() {
                return firstPart;
            }

            public S getSecondPart() {
                return secondPart;
            }
        };
    }

    /**
     * convenience method that creates an instance of three-part credentials.
     * 
     * @param firstPart
     *            first part of the credentials, for example your tenant.
     * @param secondPart
     *            second part of the credentials, for example your username.
     * @param thirdPart
     *            second part of the credentials, for example your password.
     */
    public static <F, S, T> Credentials threePartCredentials(final F firstPart, final S secondPart, final T thirdPart) {
        return new ThreePartCredentials<F, S, T>() {
            public F getFirstPart() {
                return firstPart;
            }

            public S getSecondPart() {
                return secondPart;
            }

            public T getThirdPart() {
                return thirdPart;
            }
        };
    }

    // ctor is private to ensure providers only need to consider hierarchies
    // with roots defined here.
    private Credentials() {
    }

    /**
     * Encapsulate providers that use two pieces of authenticating information,
     * such as access key and secret key or username and password. Abstract so
     * that fields can be named appropriately for serialization purposes.
     * 
     * @param <F>
     *            type of the first part, for example String for a username
     * @param <S>
     *            type of the second part, for example {@code PrivateKey}
     */
    public static abstract class TwoPartCredentials<F, S> extends Credentials {

        protected TwoPartCredentials() {
        }

        /**
         * for example access key or username
         */
        public abstract F getFirstPart();

        /**
         * for example secret key or password
         */
        public abstract S getSecondPart();

        @Override
        public int hashCode() {
            return Objects.hashCode(getFirstPart(), getSecondPart());
        }

        /**
         * equals check here doesn't require classes to be the same. This allows
         * subclasses to inherit the equals method and have {@code equals}
         * return true when the instance is the same, but for example from
         * properties vs from memory.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof TwoPartCredentials))
                return false;
            TwoPartCredentials<?, ?> that = TwoPartCredentials.class.cast(obj);
            return equal(getFirstPart(), that.getFirstPart()) && equal(getSecondPart(), that.getSecondPart());
        }
    }

    /**
     * Encapsulate providers that use three pieces of authenticating
     * information, such as a renewable set of two part credentials, like amazon
     * temporary credentials. Abstract so that fields can be named appropriately
     * for serialization purposes.
     * 
     * @param <F>
     *            type of the first part, for example String for a tenant id
     * @param <S>
     *            type of the second part, for example String for a username
     * @param <T>
     *            type of the third part, for example {@code PrivateKey}
     */
    public static abstract class ThreePartCredentials<F, S, T> extends Credentials {

        protected ThreePartCredentials() {
        }

        /**
         * for example tenantid or renew token
         */
        public abstract F getFirstPart();

        /**
         * for example access key or username
         */
        public abstract S getSecondPart();

        /**
         * for example secret key or password
         */
        public abstract T getThirdPart();

        @Override
        public int hashCode() {
            return Objects.hashCode(getFirstPart(), getSecondPart(), getThirdPart());
        }

        /**
         * equals check here doesn't require classes to be the same. This allows
         * subclasses to inherit the equals method and have {@code equals}
         * return true when the instance is the same, but for example from
         * properties vs from memory.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ThreePartCredentials))
                return false;
            ThreePartCredentials<?, ?, ?> that = ThreePartCredentials.class.cast(obj);
            return equal(getFirstPart(), that.getFirstPart()) && equal(getSecondPart(), that.getSecondPart())
                    && equal(getThirdPart(), that.getThirdPart());
        }
    }
}