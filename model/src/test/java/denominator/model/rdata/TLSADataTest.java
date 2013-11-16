package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.tlsa;

import org.testng.annotations.Test;

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
