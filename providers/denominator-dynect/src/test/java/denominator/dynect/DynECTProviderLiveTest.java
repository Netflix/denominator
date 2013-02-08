package denominator.dynect;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.staticCredentials;
import static java.lang.System.getProperty;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;
import denominator.Denominator;

@Test
public class DynECTProviderLiveTest extends BaseProviderLiveTest {
    @BeforeClass
    private void setUp() {
        String customer = emptyToNull(getProperty("dynect.customer"));
        String username = emptyToNull(getProperty("dynect.username"));
        String password = emptyToNull(getProperty("dynect.password"));
        if (customer != null && username != null && password != null) {
            manager = Denominator.create(new DynECTProvider(), staticCredentials(customer, username, password));
        }
    }
}
