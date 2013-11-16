package denominator.model.rdata;

import static denominator.model.ResourceRecordSets.txt;

import org.testng.annotations.Test;

@Test
public class TXTDataTest {

    public void testSinglePart() {
    	txt("www.denominator.io.", "foo");
    }    
    
    public void testMultiPart() {
        txt("www.denominator.io.", "\"foo bar\"");
    }

}
