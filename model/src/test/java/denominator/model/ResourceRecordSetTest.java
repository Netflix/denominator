package denominator.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import denominator.model.rdata.AData;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceRecordSetTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  ResourceRecordSet<AData> record = ResourceRecordSet.<AData>builder()
      .name("www.denominator.io.")
      .type("A")
      .ttl(3600)
      .add(AData.create("192.0.2.1")).build();

  @Test
  public void canBuildARecordSetInLongForm() {
    assertThat(record.name()).isEqualTo("www.denominator.io.");
    assertThat(record.type()).isEqualTo("A");
    assertThat(record.ttl()).isEqualTo(3600);
    assertThat(record.records().get(0)).isEqualTo(AData.create("192.0.2.1"));
  }

  @Test
  public void testNullRdataNPE() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("record");

    ResourceRecordSet.<AData>builder().add(null);
  }

  @Test

  public void testInvalidTTL() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Invalid ttl");

    ResourceRecordSet.<AData>builder()//
        .name("www.denominator.io.")//
        .type("A")//
        .ttl(0xFFFFFFFF)//
        .add(AData.create("192.0.2.1")).build();
  }
}
