package denominator.profile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import denominator.DNSApiManager;
import denominator.Live;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(Live.class)
public class WeightedReadOnlyLiveTest {

  @Parameter
  public DNSApiManager manager;

  @Test
  public void testListRRSs() {
    for (Zone zone : manager.api().zones()) {
      for (ResourceRecordSet<?> weightedRRS : weightedApi(zone)) {
        assertThat(weightedRRS).isValidWeighted();

        Weighted weighted = weightedRRS.weighted();
        assertThat(weightedApi(zone).supportedWeights())
            .contains(weighted.weight());
        assertThat(manager.provider().profileToRecordTypes().get("weighted"))
            .contains(weightedRRS.type());

        assertThat(weightedApi(zone).iterateByNameAndType(weightedRRS.name(), weightedRRS.type()))
            .overridingErrorMessage("could not list by name and type: " + weightedRRS)
            .isNotEmpty();

        ResourceRecordSet<?> byNameTypeAndQualifier = weightedApi(zone).getByNameTypeAndQualifier(
            weightedRRS.name(), weightedRRS.type(), weightedRRS.qualifier());

        assertThat(byNameTypeAndQualifier)
            .overridingErrorMessage("could not lookup by name, type, and qualifier: " + weightedRRS)
            .isNotNull()
            .isEqualTo(weightedRRS);
      }
    }
  }

  @Test
  public void testListByName() {
    for (Zone zone : manager.api().zones()) {
      Iterator<ResourceRecordSet<?>> weightedRRSIterator = weightedApi(zone).iterator();
      if (!weightedRRSIterator.hasNext()) {
        continue;
      }
      ResourceRecordSet<?> weightedRRSet = weightedRRSIterator.next();
      String name = weightedRRSet.name();
      List<ResourceRecordSet<?>> withName = new ArrayList<ResourceRecordSet<?>>();
      withName.add(weightedRRSet);
      while (weightedRRSIterator.hasNext()) {
        weightedRRSet = weightedRRSIterator.next();
        if (!name.equalsIgnoreCase(weightedRRSet.name())) {
          break;
        }
        withName.add(weightedRRSet);
      }

      assertThat(weightedApi(zone).iterateByName(name)).containsAll(withName);
      break;
    }
  }

  @Test
  public void testListByNameWhenNotFound() {
    for (Zone zone : manager.api().zones()) {
      assertThat(weightedApi(zone).iterateByName("ARGHH." + zone.name())).isEmpty();
      break;
    }
  }

  @Test
  public void testListByNameAndTypeWhenNone() {
    for (Zone zone : manager.api().zones()) {
      assertThat(weightedApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT")).isEmpty();
      break;
    }
  }

  @Test
  public void testGetByNameTypeAndQualifierWhenAbsent() {
    for (Zone zone : manager.api().zones()) {
      assertThat(weightedApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"))
          .isNull();
      break;
    }
  }

  // TODO
  protected WeightedResourceRecordSetApi weightedApi(Zone zone) {
    WeightedResourceRecordSetApi
        weightedOption =
        manager.api().weightedRecordSetsInZone(zone.id());
    assumeTrue("weighted not available or not available in zone " + zone, weightedOption != null);
    return weightedOption;
  }
}
