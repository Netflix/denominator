package denominator.route53;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRoundRobinLiveTest;

@Test
public class Route53RoundRobinLiveTest extends BaseRoundRobinLiveTest {

  @BeforeClass
  private void setUp() {
    Route53Connection connection = new Route53Connection();
    manager = connection.manager;
    setMutableZoneIfPresent(connection.mutableZone);
  }
}
