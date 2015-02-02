package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.ds;

@Test
public class DSDataTest {

  public void testGoodRecord() {
    ds("www.denominator.io.", "12345 1 1 B33F");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*record must have exactly four parts.*")
  public void testMissingParts() {
    ds("www.denominator.io.", "12345 1 1");
  }

}
