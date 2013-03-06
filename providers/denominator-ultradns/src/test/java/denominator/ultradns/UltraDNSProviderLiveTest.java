package denominator.ultradns;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;
import denominator.Denominator;

@Test(singleThreaded = true)
public class UltraDNSProviderLiveTest extends BaseProviderLiveTest {

    @BeforeClass
    private void setUp() {
        String username = emptyToNull(getProperty("ultradns.username"));
        String password = emptyToNull(getProperty("ultradns.password"));
        if (username != null && password != null) {
            manager = Denominator.create(new UltraDNSProvider(), credentials(username, password));
        }
        mutableZone = emptyToNull(getProperty("ultradns.zone"));
    }
}
