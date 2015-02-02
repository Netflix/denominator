package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.ns;

@Test
public class NSDataTest {

  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "record")
  public void testNullTargetNS() {
    ns("www.denominator.io.", (String) null);
  }

}
