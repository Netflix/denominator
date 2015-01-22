package denominator.verisignmdns;

import static denominator.verisignmdns.VrsnMDNSTest.VALID_OWNER1;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_RR_TYPE3;
import static denominator.verisignmdns.VrsnMDNSTest.VALID_TTL1;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.testng.annotations.Test;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.NAPTRData;
import denominator.verisignmdns.VrsnContentConversionHelper;
import denominator.verisignmdns.VrsnMdns.Record;

public class VrsnContentConversionHelperTest {

    @Test
    public void convertMDNSRecordToDenominator() throws IOException {
        Record aMDNSRecord = VrsnMDNSTest.mockRecord();
        ResourceRecordSet<?> rrSet = VrsnContentConversionHelper.convertMDNSRecordToResourceRecordSet(aMDNSRecord);

        assertNotNull(rrSet);
        assertEquals(rrSet.ttl(), new Integer(Integer.parseInt(VALID_TTL1)));
        assertEquals(rrSet.type(), VALID_RR_TYPE3);
        assertEquals(rrSet.name(), VALID_OWNER1);
        Object entry = rrSet.records().get(0);
        assertTrue(entry instanceof NAPTRData);
    }

    @Test
    public void getSortedSetForDenominator() {
        List<Record> aMDNSRecordList = new ArrayList<Record>();
        aMDNSRecordList.add(VrsnMDNSTest.mockRecord());

        SortedSet<ResourceRecordSet<?>> actualResult = VrsnContentConversionHelper
                .getSortedSetForDenominator(aMDNSRecordList);

        assertNotNull(actualResult);
        assertEquals(actualResult, actualResult);
    }
}
