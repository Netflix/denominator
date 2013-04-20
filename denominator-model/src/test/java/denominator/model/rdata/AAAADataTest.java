package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.aaaa;

import org.testng.annotations.Test;

@Test
public class AAAADataTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*should be a ipv6 address.*")
    public void testBadIPv4() {
        aaaa("www.denominator.io.", "192.0.2.1");
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*not an IP.*")
    public void testNoIP() {
        aaaa("www.denominator.io.", "");
    }
    
    public void testGoodIPv6() {
        aaaa("www.denominator.io.", "2001:db8:1cfe:face:b00c::3");
    }
}
