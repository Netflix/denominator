package denominator.dynect;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;

@Test
public class DynECTCheckConnectionLiveTest extends BaseProviderLiveTest {

    @BeforeClass
    private void setUp() {
        manager = new DynECTConnection().manager;
    }

    @Test
    public void success() {
        skipIfNoCredentials();
        assertTrue(manager.checkConnection());
    }

    @Test
    public void failGracefullyOnBadPassword() {
        skipIfNoCredentials();
        assertFalse(DynECTConnection.create("CUSTOMER", "FOO", "BAR").checkConnection());
    }
}
