package denominator.clouddns;

import com.google.common.collect.ImmutableMap;

import org.testng.annotations.Test;

import denominator.clouddns.RackspaceApis.Record;

import static org.testng.Assert.assertEquals;

@Test
public class CloudDNSFunctionsTest {

  public void transformsNSRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "NS";
    input.ttl = 3600;
    input.data("dns1.stabletransit.com");

    assertEquals(CloudDNSFunctions.toRDataMap(input), ImmutableMap.<String, String>of(
        "nsdname", "dns1.stabletransit.com"));
  }

  public void transformsTXTRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "TXT";
    input.ttl = 3600;
    input.data("Hello DNS");

    assertEquals(CloudDNSFunctions.toRDataMap(input), ImmutableMap.<String, String>of(
        "txtdata", "Hello DNS"));
  }
}
