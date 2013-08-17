package denominator.designate;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import denominator.designate.Designate.Record;

@Test
public class DesignateFunctionsTest {

    public void transformsNSRecordSet() {
        Record input = new Record();
        input.name = "denominator.io";
        input.type = "NS";
        input.ttl = 3600;
        input.data = "dns1.stabletransit.com";              

        assertEquals(DesignateFunctions.toRDataMap(input), ImmutableMap.<String, String> of(
        		"nsdname", "dns1.stabletransit.com"));
    }

    public void transformsTXTRecordSet() {
        Record input = new Record();
        input.name = "denominator.io";
        input.type = "TXT";
        input.ttl = 3600;
        input.data = "Hello DNS";                

        assertEquals(DesignateFunctions.toRDataMap(input), ImmutableMap.<String, String> of(
        		"txtdata", "Hello DNS"));
    }
}
