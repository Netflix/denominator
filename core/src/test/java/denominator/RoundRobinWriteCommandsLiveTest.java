package denominator;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static java.lang.String.format;
import static org.junit.Assume.assumeFalse;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Live.Write.class)
@Live.Write.Profile("roundRobin")
public class RoundRobinWriteCommandsLiveTest {

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
        .containsOnlyRecords(expected.records().get(0));
  }

  @Test
  public void test2_putAddingRData() {
    rrsApi(zone).put(ResourceRecordSet.builder()
                         .name(expected.name())
                         .type(expected.type())
                         .ttl(1800)
                         .add(expected.records().get(0))
                         .add(expected.records().get(1)).build());

    ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(expected.name(), expected.type());

    assertThat(rrs)
        .hasName(expected.name())
        .hasType(expected.type())
        .hasTtl(1800)
        .containsOnlyRecords(expected.records().get(0), expected.records().get(1));
  }

  @Test
  public void test3_putChangingTTL() {
    rrsApi(zone).put(ResourceRecordSet.builder()
                         .name(expected.name())
                         .type(expected.type())
                         .ttl(200000)
                         .add(expected.records().get(0))
                         .add(expected.records().get(1)).build());

    ResourceRecordSet<?> rrs = rrsApi(zone).getByNameAndType(expected.name(), expected.type());

    assertThat(rrs)
        .hasName(expected.name())
        .hasType(expected.type())
        .hasTtl(200000)
        .containsOnlyRecords(expected.records().get(0), expected.records().get(1));
  }

  @Test
  public void test4_putRemovingRData() {
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
        .containsOnlyRecords(expected.records().get(0));
  }

  @Test
  public void test5_deleteRRS() {
    rrsApi(zone).deleteByNameAndType(expected.name(), expected.type());

    String
        failureMessage =
        format("recordset(%s, %s) still exists in %s", expected.name(), expected.type(), zone);

    assertThat(rrsApi(zone).getByNameAndType(expected.name(), expected.type()))
        .overridingErrorMessage(failureMessage)
        .isNull();

    assertThat(allApi(zone).iterateByNameAndType(expected.name(), expected.type()))
        .overridingErrorMessage(failureMessage)
        .isEmpty();

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
    assumeFalse(format("recordset(%s, %s) already exists in %s", name, type, zone),
                allApi(zone).iterateByNameAndType(name, type).hasNext());
  }
}
