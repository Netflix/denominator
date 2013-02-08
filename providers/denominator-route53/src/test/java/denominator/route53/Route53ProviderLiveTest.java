package denominator.route53;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.staticCredentials;
import static java.lang.System.getProperty;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;
import denominator.Denominator;

@Test
public class Route53ProviderLiveTest extends BaseProviderLiveTest {
    @BeforeClass
    private void setUp() {
        String accesskey = emptyToNull(getProperty("route53.accesskey"));
        String secretkey = emptyToNull(getProperty("route53.secretkey"));
        if (accesskey != null && secretkey != null) {
            manager = Denominator.create(new Route53Provider(), staticCredentials(accesskey, secretkey));
        }
    }
}
