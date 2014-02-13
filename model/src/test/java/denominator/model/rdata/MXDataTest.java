package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.mx;

import org.testng.annotations.Test;

@Test
public class MXDataTest {

    public void testGoodRecord() {
    	mx("www.denominator.io.", "1 mx1.denominator.io.");
    }    
    
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*record must have exactly two parts.*")
    public void testMissingParts() {
    	mx("www.denominator.io.", "1");
    }

}
