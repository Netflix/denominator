package denominator.route53;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseReadOnlyLiveTest;

@Test
public class Route53ReadOnlyLiveTest extends BaseReadOnlyLiveTest {

  @BeforeClass
  private void setUp() {
    manager = new Route53Connection().manager;
  }
}
