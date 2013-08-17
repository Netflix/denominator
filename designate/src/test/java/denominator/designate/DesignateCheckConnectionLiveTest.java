package denominator.designate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;

@Test
public class DesignateCheckConnectionLiveTest extends BaseProviderLiveTest {

    @BeforeClass
    private void setUp() {
        manager = new DesignateConnection().manager;
    }

    @Test
    public void success() {
        skipIfNoCredentials();
        assertTrue(manager.checkConnection());
    }

    @Test
    public void failGracefullyOnBadPassword() {
        skipIfNoCredentials();
        assertFalse(DesignateConnection.create("TENANT", "FOO", "BAR").checkConnection());
    }
}
