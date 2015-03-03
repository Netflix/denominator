package denominator.profile;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Live;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;

import static denominator.assertj.ModelAssertions.assertThat;
import static java.lang.String.format;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Live.Write.class)
@Live.Write.Profile("geo")
public class GeoWriteCommandsLiveTest {
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
    Map<String, Collection<String>> regions = geoApi(zone).supportedRegions();

    Foo foo = new Foo(regions);
    Map<String, Collection<String>>
        allButOne = new LinkedHashMap<String, Collection<String>>(regions);
    allButOne.put(foo.lastRegion, foo.exceptLastTerritory);

    Map<String, Collection<String>> onlyOne = new LinkedHashMap<String, Collection<String>>(1);
    onlyOne.put(foo.lastRegion, Arrays.asList(foo.lastTerritory));

    int i = 0;
    for (String qualifier : new String[]{qualifier1, qualifier2}) {
      assumeRRSetAbsent(zone, expected.name(), expected.type(), qualifier);

      Geo territories = Geo.create(i == 0 ? allButOne : onlyOne);

      allApi(zone).put(ResourceRecordSet.builder()
                           .name(expected.name())
                           .type(expected.type())
                           .ttl(1800)
                           .qualifier(qualifier)
                           .geo(territories)
                           .add(expected.records().get(i)).build());

      ResourceRecordSet<?>
          rrs =
          geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(),
                                                 qualifier);

      assertThat(rrs)
          .hasName(expected.name())
          .hasType(expected.type())
          .hasQualifier(qualifier)
          .hasTtl(1800)
          .hasGeo(territories)
          .containsExactlyRecords(expected.records().get(i++));
    }
  }

  @Test
  public void test2_yankTerritoryIntoAnotherRRSet() {
    ResourceRecordSet<?> rrs1 = geoApi(zone)
        .getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    Map<String, Collection<String>> regionsFrom1 = rrs1.geo().regions();
    Foo foo = new Foo(regionsFrom1);

    Map<String, Collection<String>> toYank = new LinkedHashMap<String, Collection<String>>(1);
    toYank.put(foo.lastRegion, Arrays.asList(foo.lastTerritory));

    ResourceRecordSet<?> rrs2 = geoApi(zone)
        .getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    Map<String, Collection<String>>
        plus1 =
        new LinkedHashMap<String, Collection<String>>(rrs2.geo().regions());
    if (plus1.containsKey(foo.lastRegion)) {
      List<String> moreTerritories = new ArrayList<String>(plus1.get(foo.lastRegion));
      moreTerritories.add(foo.lastTerritory);
      plus1.put(foo.lastRegion, moreTerritories);
    } else {
      plus1.put(foo.lastRegion, Arrays.asList(foo.lastTerritory));
    }

    geoApi(zone).put(ResourceRecordSet.builder()
                         .name(expected.name())
                         .type(expected.type())
                         .qualifier(qualifier2)
                         .ttl(rrs2.ttl())
                         .geo(Geo.create(plus1))
                         .addAll(rrs2.records()).build());

    rrs1 = geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    assertThat(rrs1)
        .containsRegion(foo.lastRegion, foo.exceptLastTerritory.toArray(new String[]{}));

    rrs2 = geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    assertThat(rrs2).hasGeo(Geo.create(plus1));
  }

  @Test
  public void test3_changeTTLWithAllProfileApi() {
    int i = 0;
    for (String qualifier : new String[]{qualifier1, qualifier2}) {
      ResourceRecordSet<?>
          rrs =
          geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(),
                                                 qualifier);

      int ttl = (rrs.ttl() != null ? rrs.ttl() : 60000) + 60000;
      Geo oldGeo = rrs.geo();

      allApi(zone).put(ResourceRecordSet.builder()
                           .name(expected.name())
                           .type(expected.type())
                           .ttl(ttl)
                           .qualifier(qualifier)
                           .geo(oldGeo)
                           .add(expected.records().get(i)).build());

      rrs = geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier);

      assertThat(rrs)
          .hasName(expected.name())
          .hasType(expected.type())
          .hasQualifier(qualifier)
          .hasTtl(ttl)
          .hasGeo(oldGeo)
          .containsExactlyRecords(expected.records().get(i++));
    }
  }

  @Test
  public void test4_deleteOneQualifierDoesntAffectOther() {
    geoApi(zone).deleteByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    ResourceRecordSet<?>
        rrs =
        geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    assertNull(format("recordset(%s, %s, %s) still present in %s",
                      expected.name(), expected.type(), qualifier1, zone), rrs);

    rrs = geoApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    assertThat(rrs)
        .hasName(expected.name())
        .hasType(expected.type())
        .hasQualifier(qualifier2);

    // safe to call twice
    geoApi(zone).deleteByNameTypeAndQualifier(expected.name(), expected.type(), qualifier1);

    // clean up the other one
    allApi(zone).deleteByNameAndType(expected.name(), expected.type());

    rrs = allApi(zone).getByNameTypeAndQualifier(expected.name(), expected.type(), qualifier2);

    assertNull(format("recordset(%s, %s, %s) still present in %s",
                      expected.name(), expected.type(), qualifier2, zone), rrs);
  }

  private static final class Foo {

    String lastRegion;
    String lastTerritory;
    List<String> exceptLastTerritory;

    Foo(Map<String, Collection<String>> regions) {
      for (Iterator<String> r = regions.keySet().iterator(); r.hasNext(); ) {
        lastRegion = r.next();
      }
      exceptLastTerritory = new ArrayList<String>(regions.get(lastRegion));
      lastTerritory = exceptLastTerritory.remove(exceptLastTerritory.size() - 1);
    }
  }

  // TODO
  private AllProfileResourceRecordSetApi allApi(Zone zone) {
    return manager.api().recordSetsInZone(zone.idOrName());
  }

  private GeoResourceRecordSetApi geoApi(Zone zone) {
    GeoResourceRecordSetApi geoOption = manager.api().geoRecordSetsInZone(zone.idOrName());
    assumeTrue("geo not available or not available in zone " + zone, geoOption != null);
    return geoOption;
  }

  private void assumeRRSetAbsent(Zone zone, String name, String type, String qualifier) {
    assumeFalse(format("recordset(%s, %s, %s) already exists in %s", name, type, qualifier, zone),
                allApi(zone).getByNameTypeAndQualifier(name, type, qualifier) != null);
  }
}
