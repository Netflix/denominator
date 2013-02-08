package denominator;

import static denominator.Credentials.threePartCredentials;
import static denominator.Credentials.twoPartCredentials;

import javax.inject.Singleton;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dagger.Module;
import dagger.Provides;

/**
 * use this for providers who need credentials.
 * 
 * ex. for two-part
 * 
 * <pre>
 * ultra = Denominator.create(new UltraDNSProvider(), staticCredentials(username, password));
 * route53 = Denominator.create(new Route53Provider(), staticCredentials(accesskey, secretkey));
 * </pre>
 * 
 * ex. for three-part
 * 
 * <pre>
 * dynect = Denominator.create(new DynECTProvider(), staticCredentials(customer, username, password));
 * </pre>
 * 
 * ex. for dynamic credentials
 * 
 * <pre>
 * final AWSCredentialsProvider provider = // from wherever
 * Supplier&lt;Credentials&gt; converter = new Supplier&lt;Credentials&gt;() {
 *     public Credentials get() {
 *         AWSCredentials awsCreds = provider.getCredentials();
 *         return twoPartCredentials(awsCreds.getAWSAccessKeyId(), awsCreds.getAWSSecretKey());
 *     }
 * };
 * 
 * route53 = Denominator.create(new Route53Provider(), dynamicCredentials(converter));
 * </pre>
 */
public class CredentialsConfiguration {
    private CredentialsConfiguration() {
    }

    @Module(entryPoints = DNSApiManager.class, complete = false)
    static class CredentialsSupplier {
        private final Supplier<Credentials> creds;

        private CredentialsSupplier(Supplier<Credentials> creds) {
            this.creds = creds;
        }

        @Provides
        @Singleton
        Supplier<Credentials> supplyCredentials() {
            return creds;
        }
    }

    /**
     * used to set a base case where no credentials are available or needed.
     */
    static CredentialsSupplier none() {
        return new CredentialsSupplier(Suppliers.<Credentials> ofInstance(null));
    };

    /**
     * 
     * @param firstPart
     *            first part of credentials, such as a username or accessKey
     * @param secondPart
     *            second part of credentials, such as a password or secretKey
     */
    public static <F, S> CredentialsSupplier staticCredentials(F firstPart, S secondPart) {
        return new CredentialsSupplier(Suppliers.ofInstance(twoPartCredentials(firstPart, secondPart)));
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
    public static <F, S, T> CredentialsSupplier staticCredentials(F firstPart, S secondPart, T thirdPart) {
        return new CredentialsSupplier(Suppliers.ofInstance(threePartCredentials(firstPart, secondPart, thirdPart)));
    }

    /**
     * @param credentials
     *            will always be used on the provider
     */
    public static CredentialsSupplier staticCredentials(Credentials credentials) {
        return new CredentialsSupplier(Suppliers.ofInstance(credentials));
    }

    /**
     * @param dynamicCredentials
     *            {@link Supplier#get()} will be called each time credentials
     *            are needed, facilitating runtime credential changes.
     */
    public static CredentialsSupplier dynamicCredentials(Supplier<Credentials> dynamicCredentials) {
        return new CredentialsSupplier(dynamicCredentials);
    }
}