package denominator.ultradns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRoundRobinLiveTest;

@Test
public class UltraDNSRoundRobinLiveTest extends BaseRoundRobinLiveTest {

  @BeforeClass
  private void setUp() {
    UltraDNSConnection connection = new UltraDNSConnection();
    manager = connection.manager;
    setMutableZoneIfPresent(connection.mutableZone);
  }
}
