package denominator.model.profile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

import static denominator.assertj.ModelAssertions.assertThat;

public class GeosTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  Geo geo = Geo.create(new LinkedHashMap<String, Collection<String>>() {
    {
      put("US", Arrays.asList("US-VA", "US-CA"));
      put("IM", Arrays.asList("IM"));
    }
  });

  ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData>builder()//
      .name("www.denominator.io.")//
      .type("A")//
      .qualifier("US-East")//
      .ttl(3600)//
      .add(AData.create("1.1.1.1"))//
      .geo(geo).build();

  Weighted weighted = Weighted.create(2);

  ResourceRecordSet<AData> weightedRRS = ResourceRecordSet.<AData>builder()//
      .name("www.denominator.io.")//
      .type("A")//
      .qualifier("US-East")//
      .ttl(3600)//
      .add(AData.create("1.1.1.1"))//
      .weighted(Weighted.create(2)).build();

  @Test
  public void withAdditionalRegionsIdentityWhenAlreadyHaveRegions() {
    assertThat(Geos.withAdditionalRegions(geoRRS, geo.regions())).isEqualTo(geoRRS);
  }

  @Test
  public void withAdditionalRegionsAddsNewTerritory() {
    Map<String, Collection<String>> oregon = new LinkedHashMap<String, Collection<String>>();
    oregon.put("US", Arrays.asList("US-OR"));

    ResourceRecordSet<?> withOregon = Geos.withAdditionalRegions(geoRRS, oregon);

    assertThat(withOregon)
        .containsRegion("US", "US-VA", "US-CA", "US-OR")
        .containsRegion("IM", "IM");
  }

  @Test
  public void withAdditionalRegionsAddsNewRegion() {
    Map<String, Collection<String>> gb = new LinkedHashMap<String, Collection<String>>();
    gb.put("GB", Arrays.asList("GB-SLG", "GB-LAN"));

    ResourceRecordSet<?> withGB = Geos.withAdditionalRegions(geoRRS, gb);

    assertThat(withGB)
        .containsRegion("US", "US-VA", "US-CA")
        .containsRegion("IM", "IM")
        .containsRegion("GB", "GB-SLG", "GB-LAN");
  }

  @Test
  public void withAdditionalRegionsDoesntAffectOtherProfiles() {
    ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData>builder()//
        .name("www.denominator.io.")//
        .type("A")//
        .qualifier("US-East")//
        .ttl(3600)//
        .add(AData.create("1.1.1.1"))//
        .weighted(weighted)
        .geo(geo).build();

    Map<String, Collection<String>> oregon = new LinkedHashMap<String, Collection<String>>();
    oregon.put("US", Arrays.asList("US-OR"));

    ResourceRecordSet<?> withOregon = Geos.withAdditionalRegions(geoRRS, oregon);

    assertThat(withOregon)
        .containsRegion("US", "US-VA", "US-CA", "US-OR")
        .containsRegion("IM", "IM");

    assertThat(withOregon.weighted()).isEqualTo(weighted);
  }

  @Test
  public void withAdditionalRegionsEmpty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no regions specified");

    Geos.withAdditionalRegions(geoRRS, Collections.<String, Collection<String>>emptyMap());
  }

  @Test
  public void withAdditionalRegionsNoGeoProfile() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("rrset does not include geo configuration:");

    Geos.withAdditionalRegions(weightedRRS, geo.regions());
  }
}
