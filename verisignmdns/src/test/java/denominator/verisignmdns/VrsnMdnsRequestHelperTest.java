package denominator.verisignmdns;

import static denominator.model.ResourceRecordSets.naptr;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_OWNER1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RData_NAPTR;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_TTL1;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;

import org.testng.annotations.Test;

import denominator.model.ResourceRecordSet;
import denominator.verisignmdns.VrsnMdnsRequestHelper;

public class VrsnMdnsRequestHelperTest {

    @Test
    public void getNAPTRData() throws IOException {
        String inptNAPTRDataString = "100 50 'a' 'z3950+n2l+n2c' '' cidserver.example.com.";
        ResourceRecordSet rrSet = getResourceRecordSet(inptNAPTRDataString);
        String actualNAPTRData = VrsnMdnsRequestHelper.getNAPTRData(rrSet);
        assertNotNull(actualNAPTRData);
        System.out.println("NAPTR actual data :" + actualNAPTRData);
        assertEquals(actualNAPTRData, VALID_RData_NAPTR);
    }

    @Test
    public void getNAPTRDataInputNoQuotes() throws IOException {
        String inptNAPTRDataString = "100 50 a z3950+n2l+n2c '' cidserver.example.com.";
        ResourceRecordSet rrSet = getResourceRecordSet(inptNAPTRDataString);
        String actualNAPTRData = VrsnMdnsRequestHelper.getNAPTRData(rrSet);
        assertNotNull(actualNAPTRData);
        System.out.println("NAPTR actual data :" + actualNAPTRData);
        assertEquals(actualNAPTRData, VALID_RData_NAPTR);
    }

    @Test
    public void getNAPTRDataInputDoubleQuotes() throws IOException {
        String inptNAPTRDataString = "100 50 \"a\" \"z3950+n2l+n2c\" \"\" cidserver.example.com.";
        ResourceRecordSet rrSet = getResourceRecordSet(inptNAPTRDataString);
        String actualNAPTRData = VrsnMdnsRequestHelper.getNAPTRData(rrSet);
        assertNotNull(actualNAPTRData);
        System.out.println("NAPTR actual data :" + actualNAPTRData);
        assertEquals(actualNAPTRData, VALID_RData_NAPTR);
    }

    private ResourceRecordSet getResourceRecordSet(String rDataString) {
        return naptr(VALID_OWNER1, Integer.parseInt(VALID_TTL1), rDataString);

    }
}
