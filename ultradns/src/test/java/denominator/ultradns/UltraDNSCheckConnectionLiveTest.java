package denominator.ultradns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
