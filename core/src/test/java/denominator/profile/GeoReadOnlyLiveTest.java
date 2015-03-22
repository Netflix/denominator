package denominator.profile;

import org.assertj.core.api.Assertions;
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

import static denominator.assertj.ModelAssertions.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(Live.class)
public class GeoReadOnlyLiveTest {

  @Parameter
  public DNSApiManager manager;

  @Test
  public void testListRRSs() {
    for (Zone zone : manager.api().zones()) {
      for (ResourceRecordSet<?> geoRRS : geoApi(zone)) {
        assertThat(geoRRS).isValidGeo();

        assertThat(manager.provider().profileToRecordTypes().get("geo")).contains(geoRRS.type());

        Assertions.assertThat(
            geoApi(zone).iterateByNameAndType(geoRRS.name(), geoRRS.type()))
            .overridingErrorMessage("could not list by name and type: " + geoRRS)
            .isNotEmpty();

        ResourceRecordSet<?> byNameTypeAndQualifier = geoApi(zone).getByNameTypeAndQualifier(
            geoRRS.name(), geoRRS.type(), geoRRS.qualifier());

        assertThat(byNameTypeAndQualifier)
            .overridingErrorMessage("could not lookup by name, type, and qualifier: " + geoRRS)
            .isNotNull()
            .isEqualTo(geoRRS);
      }
    }
  }

  @Test
  public void testListByName() {
    for (Zone zone : manager.api().zones()) {
      Iterator<ResourceRecordSet<?>> geoRRSIterator = geoApi(zone).iterator();
      if (!geoRRSIterator.hasNext()) {
        continue;
      }
      ResourceRecordSet<?> geoRRSet = geoRRSIterator.next();
      String name = geoRRSet.name();
      List<ResourceRecordSet<?>> withName = new ArrayList<ResourceRecordSet<?>>();
      withName.add(geoRRSet);
      while (geoRRSIterator.hasNext()) {
        geoRRSet = geoRRSIterator.next();
        if (!name.equalsIgnoreCase(geoRRSet.name())) {
          break;
        }
        withName.add(geoRRSet);
      }

      assertThat(geoApi(zone).iterateByName(name)).containsAll(withName);
      break;
    }
  }

  @Test
  public void testListByNameWhenNotFound() {
    for (Zone zone : manager.api().zones()) {
      assertThat(geoApi(zone).iterateByName("ARGHH." + zone.name())).isEmpty();
      break;
    }
  }

  @Test
  public void testListByNameAndTypeWhenNone() {
    for (Zone zone : manager.api().zones()) {
      assertThat(geoApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT")).isEmpty();
      break;
    }
  }

  @Test
  public void testGetByNameTypeAndQualifierWhenAbsent() {
    for (Zone zone : manager.api().zones()) {
      assertThat(geoApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"))
          .isNull();
      break;
    }
  }

  private GeoResourceRecordSetApi geoApi(Zone zone) {
    GeoResourceRecordSetApi geoOption = manager.api().geoRecordSetsInZone(zone.id());
    assumeTrue("geo not available or not available in zone " + zone, geoOption != null);
    return geoOption;
  }
}
