package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.srv;

@Test
public class SRVDataTest {

  public void testGoodRecord() {
    srv("www.denominator.io.", "0 1 80 www.foo.com.");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*record must have exactly four parts.*")
  public void testMissingParts() {
    srv("www.denominator.io.", "0 1 80");
  }

}
