package denominator.ultradns;

import static org.jclouds.ultradns.ws.domain.ResourceRecord.rrBuilder;
import static org.testng.Assert.assertEquals;

import java.sql.Date;
import java.util.Map;

import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;


@Test
public class UltraDNSFunctionsTest {

    ResourceRecord a = ResourceRecord.rrBuilder().name("www.foo.com.")
                                                 .type(1)
                                                 .ttl(3600)
                                                 .rdata("1.1.1.1").build();

    ResourceRecordDetail aRecord = ResourceRecordDetail.builder()
                                                           .guid("AAAAAAAAAAAA")
                                                           .zoneId("0000000000000001")
                                                           .zoneName("foo.com.")
                                                           .created(new Date(1l))
                                                           .modified(new Date(1l))
                                                           .record(a).build();

    public void toResourceRecord() {
        assertEquals(UltraDNSFunctions.toResourceRecord().apply(aRecord), a);
    }

    @DataProvider(name = "records")
    public Object[][] createData() {
        Object[][] data = new Object[3][2];
        data[0][0] = rrBuilder().name("foo.com.").ttl(3600).type(1).rdata("1.1.1.1").build();
        data[0][1] = AData.create("1.1.1.1");
        data[1][0] = rrBuilder().name("foo.com.").ttl(3600).type(28).rdata("2001:0DB8:85A3:0000:0000:8A2E:0370:7334").build();
        data[1][1] = AAAAData.create("2001:0DB8:85A3:0000:0000:8A2E:0370:7334");
        data[2][0] = rrBuilder().name("foo.com.").ttl(3600).type(5).rdata("www.foo.com.").build();
        data[2][1] = CNAMEData.create("www.foo.com.");
        return data;
    }

    @Test(dataProvider = "records")
    public void toRdataMap(ResourceRecord input, Map<String, Object> map ) {
        assertEquals(UltraDNSFunctions.toRdataMap().apply(input), map);
    }
}
