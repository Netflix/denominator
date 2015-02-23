package denominator.dynect;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRecordSetLiveTest;

@Test
public class DynECTRecordSetLiveTest extends BaseRecordSetLiveTest {

  @BeforeClass
  private void setUp() {
    DynECTConnection connection = new DynECTConnection();
    manager = connection.manager;
    setMutableZoneIfPresent(connection.mutableZone);
  }
}
