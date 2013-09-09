package denominator.dynect;

import static org.testng.Assert.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;

public class GeoResourceRecordSetsDecoderTest {
    private static final GeoResourceRecordSetsDecoder decoder = new GeoResourceRecordSetsDecoder(new Gson(),
            new CountryToRegions().provideCountriesByRegion());

    @Test
    public void decodeZoneListWithNext() throws Throwable {
        Reader response = new InputStreamReader(getClass().getResourceAsStream("/geoservice.json"));
        Map<String, Collection<ResourceRecordSet<?>>> result = new DynECTDecoder<Map<String, Collection<ResourceRecordSet<?>>>>(
                decoder).decode(response, null);

        assertEquals(result.size(), 1);
        assertEquals(result.keySet(), ImmutableSet.of("denominator.io"));
        List<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(result.get("denominator.io"));
        assertEquals(
                rrsets.get(0),
                ResourceRecordSet
                        .<CNAMEData> builder()
                        .name("srv.denominator.io")
                        .qualifier("Everywhere Else")
                        .type("CNAME")
                        .qualifier("Everywhere Else")
                        .ttl(300)
                        .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
                        .geo(Geo.create(ImmutableMultimap.<String, String> builder().put("11", "11").put("16", "16")
                                .put("12", "12").put("17", "17").put("15", "15").put("14", "14").build().asMap()))
                        .build());
        assertEquals(
                rrsets.get(1),
                ResourceRecordSet.<CNAMEData> builder().name("srv.denominator.io").type("CNAME").qualifier("Europe")
                        .ttl(300).add(CNAMEData.create("srv-000000001.eu-west-1.elb.amazonaws.com."))
                        .geo(Geo.create(ImmutableMultimap.of("13", "13").asMap())).build());
        assertEquals(
                rrsets.get(2),
                ResourceRecordSet
                        .<CNAMEData> builder()
                        .name("srv.denominator.io")
                        .type("CNAME")
                        .qualifier("Fallback")
                        .ttl(300)
                        .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
                        .geo(Geo.create(ImmutableMultimap.<String, String> builder().put("Unknown IP", "@!")
                                .put("Fallback", "@@").build().asMap())).build());
    }
}
