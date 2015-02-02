package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.tlsa;

@Test
public class TLSADataTest {

  public void testGoodRecord() {
    tlsa("www.denominator.io.", "1 1 1 B33F");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*record must have exactly four parts.*")
  public void testMissingParts() {
    tlsa("www.denominator.io.", "1 1 1");
  }

}
