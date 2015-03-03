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

@RunWith(Live.class)
public class ReadOnlyLiveTest {

  @Parameter
  public DNSApiManager manager;

  @Test
  public void testListRRSs() {
    for (Zone zone : manager.api().zones()) {
      for (ResourceRecordSet<?> rrs : allApi(zone)) {
        assertThat(rrs).isValid();
        checkListByNameAndTypeConsistent(zone, rrs);
        if (rrs.qualifier() != null) {
          checkGetByNameTypeAndQualifierConsistent(zone, rrs);
        }
      }
    }
  }

  protected void checkGetByNameTypeAndQualifierConsistent(Zone zone, ResourceRecordSet<?> rrs) {
    ResourceRecordSet<?>
        byNameTypeAndQualifier =
        allApi(zone).getByNameTypeAndQualifier(rrs.name(), rrs.type(),
                                               rrs.qualifier());
    assertThat(byNameTypeAndQualifier)
        .describedAs("could not lookup by name, type, and qualifier: " + rrs)
        .isEqualTo(rrs);
  }

  protected void checkListByNameAndTypeConsistent(Zone zone, ResourceRecordSet<?> rrs) {
    Iterator<ResourceRecordSet<?>>
        byNameAndType = allApi(zone).iterateByNameAndType(rrs.name(), rrs.type());
    assertThat(byNameAndType)
        .describedAs(rrs + " not found in list by name and type: " + byNameAndType)
        .contains(rrs);
  }

  @Test
  public void testListByName() {
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
  public void testListByNameWhenNotFound() {
    for (Zone zone : manager.api().zones()) {
      assertThat(allApi(zone).iterateByName("ARGHH." + zone.name())).isEmpty();
      break;
    }
  }

  @Test
  public void testListByNameAndTypeWhenEmpty() {
    for (Zone zone : manager.api().zones()) {
      assertThat(allApi(zone).iterateByNameAndType("ARGHH." + zone.name(), "TXT")).isEmpty();
      break;
    }
  }

  @Test
  public void testGetByNameTypeAndGroupWhenAbsent() {
    for (Zone zone : manager.api().zones()) {
      assertThat(allApi(zone).getByNameTypeAndQualifier("ARGHH." + zone.name(), "TXT", "Mars"))
          .isNull();
      break;
    }
  }

  // TODO
  private AllProfileResourceRecordSetApi allApi(Zone zone) {
    return manager.api().recordSetsInZone(zone.idOrName());
  }
}
