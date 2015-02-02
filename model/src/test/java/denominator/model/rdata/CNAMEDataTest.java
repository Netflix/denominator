package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.cname;

@Test
public class CNAMEDataTest {

  @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "record")
  public void testNullTarget() {
    cname("www.denominator.io.", (String) null);
  }

}
