package denominator;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Live.Write.class)
public class WriteCommandsLiveTest {

  @Parameter(0)
  public DNSApiManager manager;
  @Parameter(1)
  public Zone zone;
  @Parameter(2)
  public ResourceRecordSet<?> expected;

  @Test
  public void test1_putNewRRS() {
    assumeRRSetAbsent(zone, expected.name(), expected.type());

    rrsApi(zone).put(ResourceRecordSet.builder()
                         .name(expected.name())
                         .type(expected.type())
                         .ttl(1800)
                         .add(expected.records().get(0)).build());

    ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(expected.name(), expected.type());

    assertThat(rrs)
        .hasName(expected.name())
        .hasType(expected.type())
        .hasTtl(1800)
        .containsExactlyRecords(expected.records().get(0));
  }

  @Test
  public void test2_putChangingTTL() {
    rrsApi(zone).put(ResourceRecordSet.builder()
                         .name(expected.name())
                         .type(expected.type())
                         .ttl(200000)
                         .add(expected.records().get(0)).build());

    ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(expected.name(), expected.type());

    assertThat(rrs)
        .hasName(expected.name())
        .hasType(expected.type())
        .hasTtl(200000)
        .containsExactlyRecords(expected.records().get(0));
  }

  @Test
  public void test3_deleteRRS() {
    rrsApi(zone).deleteByNameAndType(expected.name(), expected.type());

    String
        failureMessage =
        String.format("recordset(%s, %s) still exists in %s", expected.name(), expected.type(),
                      zone);
    assertTrue(failureMessage,
               rrsApi(zone).getByNameAndType(expected.name(), expected.type()) == null);
    assertFalse(failureMessage,
                allApi(zone).iterateByNameAndType(expected.name(), expected.type()).hasNext());

    // test no exception if re-applied
    rrsApi(zone).deleteByNameAndType(expected.name(), expected.type());
  }

  // TODO
  private AllProfileResourceRecordSetApi allApi(Zone zone) {
    return manager.api().recordSetsInZone(zone.id());
  }

  private ResourceRecordSetApi rrsApi(Zone zone) {
    return manager.api().basicRecordSetsInZone(zone.id());
  }

  private void assumeRRSetAbsent(Zone zone, String name, String type) {
    assumeFalse(String.format("recordset(%s, %s) already exists in %s", name, type, zone),
                allApi(zone).iterateByNameAndType(name, type).hasNext());
  }
}
