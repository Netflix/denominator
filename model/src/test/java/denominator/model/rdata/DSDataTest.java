package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.ds;

import org.testng.annotations.Test;

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
