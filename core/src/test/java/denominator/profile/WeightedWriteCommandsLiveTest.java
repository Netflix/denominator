package denominator.profile;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Live;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;

import static denominator.assertj.ModelAssertions.assertThat;
import static java.lang.String.format;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Live.Write.class)
@Live.Write.Profile("weighted")
public class WeightedWriteCommandsLiveTest {

  private final String qualifier1 = "US-East";
  private final String qualifier2 = "US-West";

  @Parameter(0)
  public DNSApiManager manager;
  @Parameter(1)
  public Zone zone;
  @Parameter(2)
  public ResourceRecordSet<?> expected;

  @Test
  public void test1_putNewRRS() {
    int i = 0;
    for (String qualifier : new String[]{qualifier1, qualifier2}) {
      assumeRRSetAbsent(zone, expected.name(), expected.type(), qualifier);

      allApi(zone).put(ResourceRecordSet.builder()
                           .name(expected.name())
                           .type(expected.type())
                           .ttl(1800)
                           .qualifier(qualifier)
                           .weighted(Weighted.create(0))
                           .add(expected.records().get(i)).build());

      ResourceRecordSet<?> rrs = weightedApi(zone)
          .getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier);

      assertThat(rrs)
          .hasName(expected.name())
          .hasType(expected.type())
          .hasQualifier(qualifier)
          .hasTtl(1800)
          .hasWeight(0)
          .containsExactlyRecords(expected.records().get(i++));
    }
  }

  @Test
  public void test2_replaceWeight() {
    int heaviest = weightedApi(zone).supportedWeights().last();

    weightedApi(zone).put(ResourceRecordSet.builder()
                              .name(expected.name())
                              .type(expected.type())
                              .ttl(1800)
                              .qualifier(qualifier1)
                              .weighted(Weighted.create(heaviest))
                              .add(expected.records().get(0)).build());

    ResourceRecordSet<?> rrs1 = weightedApi(zone)
        .getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    assertThat(rrs1).hasWeight(heaviest);

    ResourceRecordSet<?> rrs2 = weightedApi(zone)
        .getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    assertThat(rrs2).hasWeight(0);
  }

  @Test
  public void test3_deleteOneQualifierDoesntAffectOther() {
    weightedApi(zone).deleteByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    ResourceRecordSet<?> rrs = weightedApi(zone)
        .getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    assertNull(format("recordset(%s, %s, %s) still present in %s",
                      expected.name(), expected.type(), qualifier1, zone), rrs);

    rrs = weightedApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    assertThat(rrs)
        .hasName(expected.name())
        .hasType(expected.type())
        .hasQualifier(qualifier2);

    // safe to call twice
    weightedApi(zone).deleteByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    // clean up the other one
    allApi(zone).deleteByNameAndType(expected.name(), expected.type());

    rrs = allApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    assertNull(format("recordset(%s, %s, %s) still present in %s",
                      expected.name(), expected.type(), qualifier2, zone), rrs);
  }

  // TODO
  private AllProfileResourceRecordSetApi allApi(Zone zone) {
    return manager.api().recordSetsInZone(zone.id());
  }

  private WeightedResourceRecordSetApi weightedApi(Zone zone) {
    WeightedResourceRecordSetApi
        weightedOption =
        manager.api().weightedRecordSetsInZone(zone.id());
    assumeTrue("weighted not available or not available in zone " + zone, weightedOption != null);
    return weightedOption;
  }

  private void assumeRRSetAbsent(Zone zone, String name, String type, String qualifier) {
    assumeFalse(format("recordset(%s, %s, %s) already exists in %s", name, type, qualifier, zone),
                allApi(zone).getByNameTypeAndQualifier(name, type, qualifier) != null);
  }
}
