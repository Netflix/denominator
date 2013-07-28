package denominator.route53;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;

@Test
public class Route53CheckConnectionLiveTest extends BaseProviderLiveTest {

    @BeforeClass
    private void setUp() {
        manager = new Route53Connection().manager;
    }

    @Test
    public void success() {
        skipIfNoCredentials();
        assertTrue(manager.checkConnection());
    }

    @Test
    public void failGracefullyOnSecret() {
        skipIfNoCredentials();
        assertFalse(Route53Connection.create("ACCESS", "SECRET").checkConnection());
    }
}
