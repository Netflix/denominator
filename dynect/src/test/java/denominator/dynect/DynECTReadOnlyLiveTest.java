package denominator.dynect;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseReadOnlyLiveTest;

@Test
public class DynECTReadOnlyLiveTest extends BaseReadOnlyLiveTest {

  @BeforeClass
  private void setUp() {
    manager = new DynECTConnection().manager;
  }
}
