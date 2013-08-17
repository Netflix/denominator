package denominator.ultradns;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;

@Test
public class UltraDNSCheckConnectionLiveTest extends BaseProviderLiveTest {

    @BeforeClass
    private void setUp() {
        manager = new UltraDNSConnection().manager;
    }

    @Test
    public void success() {
        skipIfNoCredentials();
        assertTrue(manager.checkConnection());
    }

    @Test
    public void failGracefullyOnSecret() {
        skipIfNoCredentials();
        assertFalse(UltraDNSConnection.create("FOO", "BAR").checkConnection());
    }
}
