package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.spf;

import org.testng.annotations.Test;

@Test
public class SPFDataTest {

    public void testSinglePart() {
    	spf("www.denominator.io.", "v=spf1 a mx -all");
    }    
    
    public void testMultiPart() {
    	spf("www.denominator.io.", "\"v=spf1 a mx -all\" \"v=spf1 aaaa mx -all\"");
    }

}
