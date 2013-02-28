package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.cname;

import org.testng.annotations.Test;

@Test
public class CNAMEDataTest {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "rdata")
    public void testNullTarget() {
        cname("www.denominator.io.", (String) null);
    }
    
}
