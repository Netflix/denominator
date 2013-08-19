package denominator.model.profile;

import static denominator.model.profile.WeightedTest.weighted;
import static denominator.model.profile.WeightedTest.weightedRRS;
import static org.testng.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

@Test
public class GeosTest {
    static Geo geo = Geo.create(ImmutableMultimap.<String, String> builder()//
            .put("US", "US-VA")//
            .put("US", "US-CA")//
            .put("IM", "IM").build().asMap());

    static ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData> builder()//
            .name("www.denominator.io.")//
            .type("A")//
            .qualifier("US-East")//
            .ttl(3600)//
            .add(AData.create("1.1.1.1"))//
            .addProfile(geo).build();

    public void withAdditionalRegionsIdentityWhenAlreadyHaveRegions() {
        assertEquals(Geos.withAdditionalRegions(geoRRS, geo.regions()), geoRRS);
    }

    public void withAdditionalRegionsAddsNewTerritory() {
        ResourceRecordSet<?> withOregon = Geos.withAdditionalRegions(geoRRS, ImmutableMultimap.<String, String> builder()//
                .put("US", "US-OR").build().asMap());
        
        //TODO: switch to fest so we don't have to play games like this.
        assertEquals(Geo.asGeo(withOregon).regions().toString(), ImmutableMultimap.<String, String> builder()//
                .putAll("US", "US-VA", "US-CA", "US-OR")//
                .put("IM", "IM").build().asMap().toString());
    }

    public void withAdditionalRegionsAddsNewRegion() {
        ResourceRecordSet<?> withGB = Geos.withAdditionalRegions(geoRRS, ImmutableMultimap.<String, String> builder()//
                .putAll("GB", "GB-SLG", "GB-LAN").build().asMap());
        
        //TODO: switch to fest so we don't have to play games like this.
        assertEquals(Geo.asGeo(withGB).regions().toString(), ImmutableMultimap.<String, String> builder()//
                .putAll("US", "US-VA", "US-CA")//
                .put("IM", "IM")//
                .putAll("GB", "GB-SLG", "GB-LAN").build().asMap().toString());
    }

    public void withAdditionalRegionsDoesntAffectOtherProfiles() {
        ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData> builder()//
                .name("www.denominator.io.")//
                .type("A")//
                .qualifier("US-East")//
                .ttl(3600)//
                .add(AData.create("1.1.1.1"))//
                .addProfile(weighted)
                .addProfile(geo).build();

        ResourceRecordSet<?> withOregon = Geos.withAdditionalRegions(geoRRS, ImmutableMultimap.<String, String> builder()//
                .put("US", "US-OR").build().asMap());
        
        //TODO: switch to fest so we don't have to play games like this.
        assertEquals(Geo.asGeo(withOregon).regions().toString(), ImmutableMultimap.<String, String> builder()//
                .putAll("US", "US-VA", "US-CA", "US-OR")//
                .put("IM", "IM").build().asMap().toString());

        assertEquals(Weighted.asWeighted(withOregon), weighted);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no regions specified")
    public void withAdditionalRegionsEmpty() {
         Geos.withAdditionalRegions(geoRRS, Collections.<String, Collection<String>> emptyMap());
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "rrset does not include geo configuration:.*")
    public void withAdditionalRegionsNoGeoProfile() {
         Geos.withAdditionalRegions(weightedRRS, geo.regions());
    }
}
