package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseReadOnlyLiveTest;

@Test
public class MockReadOnlyLiveTest extends BaseReadOnlyLiveTest {

  @BeforeClass
  private void setUp() {
    manager = new MockConnection().manager;
  }
}
