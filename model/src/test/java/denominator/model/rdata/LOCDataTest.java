package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.loc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class LOCDataTest {

  public void testGoodRecord() {
    loc("www.denominator.io.", "37 48 48.892 S 144 57 57.502 E 26m 10m 100m 10m");
  }

  public void testSimpleRecord() {
    loc("www.denominator.io.", "37 48 48.892 S 144 57 57.502 E 0m");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*could not find longitude.*")
  public void testMissingParts() {
    loc("www.denominator.io.", "37 48 48.892 S");
  }

  public void testSimple() {
    LOCData data = LOCData.create("37 48 48.892 S 144 57 57.502 E 0m");
    assertEquals(data.latitude(), "37 48 48.892 S");
    assertEquals(data.longitude(), "144 57 57.502 E");
    assertEquals(data.altitude(), "0m");
    assertNull(data.diameter());
    assertNull(data.hprecision());
    assertNull(data.vprecision());
  }

  public void testMinimal() {
    LOCData data = LOCData.create("37 S 144 E 0m");
    assertEquals(data.latitude(), "37 S");
    assertEquals(data.longitude(), "144 E");
    assertEquals(data.altitude(), "0m");
    assertNull(data.diameter());
    assertNull(data.hprecision());
    assertNull(data.vprecision());
  }

  public void testFull() {
    LOCData data = LOCData.create("37 48 48.892 S 144 57 57.502 E 26m 1m 2m 3m");
    assertEquals(data.latitude(), "37 48 48.892 S");
    assertEquals(data.longitude(), "144 57 57.502 E");
    assertEquals(data.altitude(), "26m");
    assertEquals(data.diameter(), "1m");
    assertEquals(data.hprecision(), "2m");
    assertEquals(data.vprecision(), "3m");
  }

}
