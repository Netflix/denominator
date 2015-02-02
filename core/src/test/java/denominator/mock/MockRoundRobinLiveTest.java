package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRoundRobinLiveTest;

@Test
public class MockRoundRobinLiveTest extends BaseRoundRobinLiveTest {

  @BeforeClass
  private void setUp() {
    MockConnection connection = new MockConnection();
    manager = connection.manager;
    mutableZone = manager.api().zones().iterator().next();
  }
}
