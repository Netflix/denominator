package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.a;

import org.testng.annotations.Test;

@Test
public class ADataTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*should be a ipv4 address.*")
    public void testBadIPv6() {
        a("www.denominator.io.", "2001:db8:1cfe:face:b00c::3");
    }    
    
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*not an IP.*")
    public void testNoIP() {
        a("www.denominator.io.", "");
    }
    
    public void testGoodIPv4() {
        a("www.denominator.io.", "192.0.2.1");
    }
}
