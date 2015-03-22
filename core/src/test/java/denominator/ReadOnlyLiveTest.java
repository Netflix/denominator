package denominator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(Live.class)
public class ReadOnlyLiveTest {

  @Parameter
  public DNSApiManager manager;

  @Test
  public void zoneIdentification() {
    Iterator<Zone> zones = manager.api().zones().iterator();
    assumeTrue("No zones to test", zones.hasNext());
    Zone zone = zones.next();
    switch (manager.provider().zoneIdentification()) {
      case NAME:
        assertThat(zone).hasNoQualifier().hasId(zone.name());
        break;
      case OPAQUE:
        assertThat(zone).hasNoQualifier();
        assertThat(zone.id()).isNotEqualTo(zone.name());
        break;
      case QUALIFIED:
        assertThat(zone.qualifier()).isNotNull();
        assertThat(zone.id()).isNotEqualTo(zone.name());
        break;
      default:
        throw new AssertionError("unknown zone identification");
    }
  }

  @Test
  public void iterateZonesByName() {
    Iterator<Zone> zones = manager.api().zones().iterator();
    assumeTrue("No zones to test", zones.hasNext());
    Zone zone = zones.next();
    assertThat(manager.api().zones().iterateByName(zone.name())).contains(zone);
  }

  @Test
  public void iterateZonesByNameWhenNotFound() {
    assertThat(manager.api().zones().iterateByName("ARGHH")).isEmpty();
  }

  @Test
  public void iterateRRSets() {
    for (Zone zone : manager.api().zones()) {
      for (ResourceRecordSet<?> rrs : allApi(zone)) {
        assertThat(rrs).isValid();
        checkIterateByNameAndTypeConsistent(zone, rrs);
        if (rrs.qualifier() != null) {
          checkGetByNameTypeAndQualifierConsistent(zone, rrs);
        }
      }
    }
  }

  @Test
  public void iterateRRSetsByName() {
    for (Zone zone : manager.api().zones()) {
      Iterator<ResourceRecordSet<?>> rrsIterator = allApi(zone).iterator();
      if (!rrsIterator.hasNext()) {
        continue;
      }
      ResourceRecordSet<?> rrset = rrsIterator.next();
      String name = rrset.name();
      List<ResourceRecordSet<?>> withName = new ArrayList<ResourceRecordSet<?>>();
      withName.add(rrset);
      while (rrsIterator.hasNext()) {
        rrset = rrsIterator.next();
        if (!name.equalsIgnoreCase(rrset.name())) {
          break;
        }
        withName.add(rrset);
      }
      Iterator<ResourceRecordSet<?>> fromApi = allApi(zone).iterateByName(name);
      assertThat(fromApi).containsAll(withName);
      break;
    }
  }

  @Test
  public void iterateRRSetsByNameWhenNotFound() {
    for (Zone zone : manager.api().zones()) {
      assertThat(allApi(zone).iterateByName("ARGHH." + zone.name())).isEmpty();
      break;
    }
  }

  @Test
  public void iterateRRSetsByNameAndTypeWhenEmpty() {
    for (Zone zone : manager.api().zones()) {
      assertThat(allApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT")).isEmpty();
      break;
    }
  }

  @Test
  public void getByNameTypeAndGroupWhenAbsent() {
    for (Zone zone : manager.api().zones()) {
      assertThat(allApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"))
          .isNull();
      break;
    }
  }

  // TODO
  private void checkGetByNameTypeAndQualifierConsistent(Zone zone, ResourceRecordSet<?> rrs) {
    ResourceRecordSet<?>
        byNameTypeAndQualifier =
        allApi(zone).getByNameTypeAndQualifier(rrs.name(), rrs.type(),
                                               rrs.qualifier());
    assertThat(byNameTypeAndQualifier)
        .describedAs("could not lookup by name, type, and qualifier: " + rrs)
        .isEqualTo(rrs);
  }

  private void checkIterateByNameAndTypeConsistent(Zone zone, ResourceRecordSet<?> rrs) {
    Iterator<ResourceRecordSet<?>>
        byNameAndType = allApi(zone).iterateByNameAndType(rrs.name(), rrs.type());
    assertThat(byNameAndType)
        .describedAs(rrs + " not found in list by name and type: " + byNameAndType)
        .contains(rrs);
  }

  private AllProfileResourceRecordSetApi allApi(Zone zone) {
    return manager.api().recordSetsInZone(zone.id());
  }
}
