package denominator;

import static denominator.Credentials.threePartCredentials;
import static denominator.Credentials.twoPartCredentials;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.base.Supplier;

@Test
public class CredentialsTest {

    public void testTwoPartCredentialsEqualsHashCode() {
        assertEquals(twoPartCredentials("user", "pass"), twoPartCredentials("user", "pass"));
        assertEquals(twoPartCredentials("user", "pass").hashCode(), twoPartCredentials("user", "pass").hashCode());
    }

    public void testThreePartCredentialsEqualsHashCode() {
        assertEquals(threePartCredentials("customer", "user", "pass"), threePartCredentials("customer", "user", "pass"));
        assertEquals(threePartCredentials("customer", "user", "pass").hashCode(),
                threePartCredentials("customer", "user", "pass").hashCode());
    }

    private static enum AWSCredentials {
        INSTANCE;
        String getAWSAccessKeyId() {
            return "accessKey";
        }

        String getAWSSecretKey() {
            return "secretKey";
        }
    }

    private static class AWSCredentialsProvider {
        AWSCredentials getCredentials() {
            return AWSCredentials.INSTANCE;
        }
    }

    public void testHowToConvertSomethingLikeAmazon() {
        final AWSCredentialsProvider provider = new AWSCredentialsProvider();
        Supplier<Credentials> converter = new Supplier<Credentials>() {
            public Credentials get() {
                AWSCredentials awsCreds = provider.getCredentials();
                return twoPartCredentials(awsCreds.getAWSAccessKeyId(), awsCreds.getAWSSecretKey());
            }
        };
        assertEquals(converter.get(), twoPartCredentials("accessKey", "secretKey"));
    }
}
