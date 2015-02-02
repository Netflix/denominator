package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.sshfp;

@Test
public class SSHFPDataTest {

  public void testGoodRecord() {
    sshfp("www.denominator.io.", "1 1 B33F");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*record must have exactly three parts.*")
  public void testMissingParts() {
    sshfp("www.denominator.io.", "1");
  }

}
