package denominator.designate;

import org.testng.annotations.Test;

import denominator.designate.Designate.Record;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class DesignateFunctionsTest {

  public void transformsNSRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "NS";
    input.ttl = 3600;
    input.data = "dns1.stabletransit.com";

    assertThat(DesignateFunctions.toRDataMap(input))
        .containsEntry("nsdname", "dns1.stabletransit.com");
  }

  public void transformsTXTRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "TXT";
    input.ttl = 3600;
    input.data = "Hello DNS";

    assertThat(DesignateFunctions.toRDataMap(input))
        .containsEntry("txtdata", "Hello DNS");
  }
}
