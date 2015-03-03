package denominator.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import denominator.model.rdata.AData;

import static denominator.assertj.ModelAssertions.assertThat;

public class ResourceRecordSetTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void canBuildARecordSetInLongForm() {
    ResourceRecordSet<AData> record = ResourceRecordSet.<AData>builder()
        .name("www.denominator.io.")
        .type("A")
        .ttl(3600)
        .add(AData.create("192.0.2.1")).build();

    assertThat(record)
        .hasName("www.denominator.io.")
        .hasType("A")
        .hasTtl(3600)
        .containsExactlyRecords(AData.create("192.0.2.1"));
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
