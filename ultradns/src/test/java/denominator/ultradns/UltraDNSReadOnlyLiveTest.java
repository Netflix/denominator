package denominator.ultradns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseReadOnlyLiveTest;

@Test
public class UltraDNSReadOnlyLiveTest extends BaseReadOnlyLiveTest {

  @BeforeClass
  private void setUp() {
    manager = new UltraDNSConnection().manager;
  }
}
