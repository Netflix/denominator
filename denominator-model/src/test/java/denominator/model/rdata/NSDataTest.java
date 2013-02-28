package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.ns;

import org.testng.annotations.Test;

@Test
public class NSDataTest {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "rdata")
    public void testNullTargetNS() {
        ns("www.denominator.io.", (String) null);
    }
    
}
