package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.a;

import org.testng.annotations.Test;

@Test
public class ADataTest {

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testBadIPv6() {
        a("www.denominator.io.", "2620:0:1cfe:face:b00c::3");
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testNoIP() {
        a("www.denominator.io.", "");
    }
    
    public void testGoodIPv4() {
        a("www.denominator.io.", "192.168.254.5");
    }
}
