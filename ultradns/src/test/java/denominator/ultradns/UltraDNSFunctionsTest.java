package denominator.ultradns;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Map;

import denominator.ResourceTypeToValue;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.ultradns.UltraDNS.Record;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class UltraDNSFunctionsTest {

  @Parameterized.Parameter(0)
  public Record input;
  @Parameterized.Parameter(1)
  public Map<String, Object> map;

  @Test
  public void toRdataMap() {
    String type = ResourceTypeToValue.lookup(input.typeCode);
    assertThat(UltraDNSFunctions.forTypeAndRData(type, input.rdata)).isEqualTo(map);
  }

  @Parameters
  public static Object[][] createData() {
    Object[][] data = new Object[3][2];

    Record a = new Record();
    a.name = "foo.com.";
    a.typeCode = 1;
    a.ttl = 3600;
    a.rdata.add("192.0.2.1");
    data[0][0] = a;
    data[0][1] = AData.create("192.0.2.1");

    Record aaaa = new Record();
    aaaa.name = "foo.com.";
    aaaa.typeCode = 28;
    aaaa.ttl = 3600;
    aaaa.rdata.add("2001:0DB8:85A3:0000:0000:8A2E:0370:7334");
    data[1][0] = aaaa;
    data[1][1] = AAAAData.create("2001:0DB8:85A3:0000:0000:8A2E:0370:7334");

    Record cname = new Record();
    cname.name = "foo.com.";
    cname.typeCode = 5;
    cname.ttl = 3600;
    cname.rdata.add("www.foo.com.");
    data[2][0] = cname;
    data[2][1] = CNAMEData.create("www.foo.com.");
    return data;
  }
}
