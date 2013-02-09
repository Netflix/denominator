package denominator.ultradns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.staticCredentials;
import static java.lang.System.getProperty;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;
import denominator.Denominator;

@Test
public class UltraDNSProviderLiveTest extends BaseProviderLiveTest {

    @BeforeClass
    private void setUp() {
        String username = emptyToNull(getProperty("ultradns.username"));
        String password = emptyToNull(getProperty("ultradns.password"));
        if (username != null && password != null) {
            manager = Denominator.create(new UltraDNSProvider(), staticCredentials(username, password));
        }
    }
}
