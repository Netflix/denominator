package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.aaaa;

import org.testng.annotations.Test;

@Test
public class AAAADataTest {

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testBadIPv4() {
        aaaa("www.denominator.io.", "192.168.254.5");
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testNoIP() {
        aaaa("www.denominator.io.", "");
    }
    
    public void testGoodIPv6() {
        aaaa("www.denominator.io.", "2620:0:1cfe:face:b00c::3");
    }
}
