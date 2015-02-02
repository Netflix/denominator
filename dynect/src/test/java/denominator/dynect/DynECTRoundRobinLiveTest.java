package denominator.dynect;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRoundRobinLiveTest;

@Test
public class DynECTRoundRobinLiveTest extends BaseRoundRobinLiveTest {

  @BeforeClass
  private void setUp() {
    DynECTConnection connection = new DynECTConnection();
    manager = connection.manager;
    setMutableZoneIfPresent(connection.mutableZone);
  }
}
