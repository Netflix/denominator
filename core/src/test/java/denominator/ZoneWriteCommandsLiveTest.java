package denominator;

import org.junit.AssumptionViolatedException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.ns;
import static java.lang.String.format;
import static org.junit.Assume.assumeTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Live.Write.class)
public class ZoneWriteCommandsLiveTest {

  @Parameter(0)
  public DNSApiManager manager;
  @Parameter(1)
  public String zoneName;

  @Test
  public void test1_putNewZone() {
    assumeZoneAbsent(zoneName);

    String id = zoneApi().put(Zone.create(null, zoneName, 3601, "test@denominator.io"));

    assertThat(id).isNotNull();

    Zone zone = zoneApi().iterateByName(zoneName).next();

    assertThat(zone)
        .hasName(zoneName)
        .hasId(id)
        .hasEmail("test@denominator.io")
        .hasTtl(3601);
  }

  @Test
  public void test2_zoneTtlIsEqualToSOATtl() {
    assertZoneTtlIsEqualToSOATtl();
  }

  @Test
  public void test2_createDuplicateZone() {
    Zone zone = assumeZonePresent(zoneName);

    // Regardless of whether the provider supports duplicate zones, putting an existing zone without
    // changes should be a no-op.
    String zoneId = zoneApi().put(zone);
    assertThat(zoneId).isEqualTo(zone.id());

    // When duplicate zones are supported, putting a new zone (and not supplying an id) means you
    // intend to create a duplicate zone as opposed to updating the first.
    String maybeDuplicate = zoneApi().put(Zone.create(null, zone.name(), zone.ttl(), zone.email()));

    if (manager.provider().supportsDuplicateZoneNames()) {
      assertThat(zone.id()).isNotEqualTo(maybeDuplicate);
      zoneApi().delete(maybeDuplicate);
    } else {
      assertThat(zone.id()).isEqualTo(maybeDuplicate);
    }
  }

  @Test
  public void test3_putChangingDefaultTTL() {
    Zone zone = assumeZonePresent(zoneName);

    String zoneId = zoneApi().put(Zone.create(zone.id(), zone.name(), 200000, zone.email()));
    assertThat(zoneId).isEqualTo(zone.id());

    assertThat(zoneApi().iterateByName(zoneName).next())
        .hasTtl(200000);
  }

  @Test
  public void test4_zoneTtlIsEqualToSOATtl() {
    assertZoneTtlIsEqualToSOATtl();
  }

  @Test
  public void test4_putChangingEmail() {
    Zone zone = assumeZonePresent(zoneName);

    String zoneId =
        zoneApi().put(Zone.create(zone.id(), zone.name(), zone.ttl(), "nil@denominator.io"));
    assertThat(zoneId).isEqualTo(zone.id());

    assertThat(zoneApi().iterateByName(zoneName).next())
        .hasEmail("nil@denominator.io");
  }

  @Test
  public void test5_deleteZoneWhenNotEmpty() {
    Zone zone = assumeZonePresent(zoneName);

    // Ensure even custom top-level NS records are deleted.
    manager.api().basicRecordSetsInZone(zone.id()).put(a("ns-google." + zoneName, "8.8.8.8"));
    manager.api().basicRecordSetsInZone(zone.id())
        .put(ns(zoneName, nsValues(zone, "ns-google." + zoneName)));

    // We expect all providers (even route53 which requires clearing first) to act the same.
    zoneApi().delete(zone.id());

    assertThat(zoneApi())
        .doesNotContain(zone);
    assertThat(zoneApi().iterateByName(zone.name()))
        .isEmpty();

    // deleting again is ok
    zoneApi().delete(zone.id());
  }

  private ZoneApi zoneApi() {
    return manager.api().zones();
  }

  private void assumeZoneAbsent(String zoneName) {
    Iterator<Zone> existing = zoneApi().iterateByName(zoneName);
    if (existing.hasNext()) {
      throw new AssumptionViolatedException(format("zone(%s) already exists", existing.next()));
    }
  }

  private Zone assumeZonePresent(String zoneName) {
    Iterator<Zone> existing = zoneApi().iterateByName(zoneName);
    if (!existing.hasNext()) {
      throw new AssumptionViolatedException(format("zone(%s) doesn't exist", zoneName));
    }
    return existing.next();
  }

  /**
   * @see ReadOnlyLiveTest#zoneTtlIsEqualToSOATtl()
   */
  private void assertZoneTtlIsEqualToSOATtl() {
    Zone zone = assumeZonePresent(zoneName);

    ResourceRecordSet<?>
        soa =
        manager.api().basicRecordSetsInZone(zone.id()).getByNameAndType(zone.name(), "SOA");
    assumeTrue("SOA records aren't exposed", soa != null); // Ex. designate doesn't expose SOA

    assertThat(zone.ttl())
        .overridingErrorMessage("zone %s should have the same ttl as soa %s", zone, soa)
        .isEqualTo(soa.ttl());
  }

  /**
   * Returns {@code nsdname} prefixed by any existing NS values.
   */
  private List<String> nsValues(Zone zone, String nsdname) {
    List<String> result = new ArrayList<String>();
    ResourceRecordSet<?> rrset =
        manager.api().basicRecordSetsInZone(zone.id()).getByNameAndType(zoneName, "NS");
    if (rrset != null) { // Ex. designate doesn't expose default NS records
      for (Map<String, Object> record : rrset.records()) {
        result.add(record.get("nsdname").toString());
      }
    }
    result.add(nsdname);
    return result;
  }
}
