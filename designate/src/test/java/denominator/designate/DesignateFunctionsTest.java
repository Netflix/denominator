package denominator.designate;

import org.junit.Test;

import denominator.designate.Designate.Record;

import static org.assertj.core.api.Assertions.assertThat;

public class DesignateFunctionsTest {

  @Test
  public void transformsNSRecordSet() {
    Record input = new Record();
    input.name = "denominator.io";
    input.type = "NS";
    input.ttl = 3600;
    input.data = "dns1.stabletransit.com";

    assertThat(DesignateFunctions.toRDataMap(input))
        .containsEntry("nsdname", "dns1.stabletransit.com");
  }

  @Test
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
