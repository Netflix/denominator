package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseGeoWriteCommandsLiveTest;

@Test
public class MockGeoWriteCommandsLiveTest extends BaseGeoWriteCommandsLiveTest {

  @BeforeClass
  private void setUp() {
    MockConnection connection = new MockConnection();
    manager = connection.manager;
    mutableZone = manager.api().zones().iterator().next();
  }
}
