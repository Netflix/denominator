package denominator.dynect;

import static org.testng.Assert.assertEquals;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import feign.Response;

public class GeoResourceRecordSetsDecoderTest {
    private static final GeoResourceRecordSetsDecoder decoder = new GeoResourceRecordSetsDecoder(
            new CountryToRegions().provideCountryToRegionIndexer(new CountryToRegions().provideCountriesByRegion()));

    @Test
    public void decodeZoneListWithNext() throws Throwable {
        Response response = Response.create(200, "OK", ImmutableMap.<String, Collection<String>> of(),
                new InputStreamReader(getClass().getResourceAsStream("/geoservice.json")), null);
        @SuppressWarnings({ "unchecked", "serial" })
        Multimap<String, ResourceRecordSet<?>> result = (Multimap<String, ResourceRecordSet<?>>) DynECTDecoder.parseDataWith(decoder).decode(null, response,
                new TypeToken<List<ResourceRecordSet<?>>>() {
                }.getType());

        assertEquals(result.size(), 3);
        assertEquals(result.keySet(), ImmutableSet.of("denominator.io"));
        List<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(result.get("denominator.io"));
        assertEquals(rrsets.get(0), ResourceRecordSet.<CNAMEData> builder()
                .name("srv.denominator.io")
                .qualifier("Everywhere Else")
                .type("CNAME")
                .qualifier("Everywhere Else")
                .ttl(300)
                .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
                .addProfile(Geo.create(ImmutableMultimap.<String, String> builder()
                                                        .put("11", "11")
                                                        .put("16", "16")
                                                        .put("12", "12")
                                                        .put("17", "17")
                                                        .put("15", "15")
                                                        .put("14", "14").build()))                                                   
                .build());
        assertEquals(rrsets.get(1), ResourceRecordSet.<CNAMEData> builder()
                .name("srv.denominator.io")
                .type("CNAME")
                .qualifier("Europe")
                .ttl(300)
                .add(CNAMEData.create("srv-000000001.eu-west-1.elb.amazonaws.com."))
                .addProfile(Geo.create(ImmutableMultimap.of("13", "13")))
                .build());
        assertEquals(rrsets.get(2),ResourceRecordSet.<CNAMEData> builder()
                .name("srv.denominator.io")
                .type("CNAME")
                .qualifier("Fallback")
                .ttl(300)
                .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
                .addProfile(Geo.create(ImmutableMultimap.<String, String> builder()
                                                        .put("Unknown IP", "@!")
                                                        .put("Fallback", "@@").build()))
                .build());
    }
}
