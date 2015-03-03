package denominator.clouddns;

import org.junit.Test;

import denominator.clouddns.RackspaceApis.Record;
import denominator.model.rdata.NSData;
import denominator.model.rdata.TXTData;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudDNSFunctionsTest {

  @Test
  public void transformsNSRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "NS";
    input.ttl = 3600;
    input.data("dns1.stabletransit.com");

    assertThat(CloudDNSFunctions.toRDataMap(input))
        .isEqualTo(NSData.create("dns1.stabletransit.com"));
  }

  @Test
  public void transformsTXTRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "TXT";
    input.ttl = 3600;
    input.data("Hello DNS");

    assertThat(CloudDNSFunctions.toRDataMap(input))
        .isEqualTo(TXTData.create("Hello DNS"));
  }
}
