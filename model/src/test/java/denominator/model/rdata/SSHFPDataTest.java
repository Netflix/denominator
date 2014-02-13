package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.sshfp;

import org.testng.annotations.Test;

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
