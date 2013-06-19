package denominator.model.profile;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

@Test
public class GeoTest {

    Geo geo = Geo.create(ImmutableMultimap.<String, String> builder()//
            .put("US", "US-VA")//
            .put("US", "US-CA")//
            .put("IM", "IM").build());

    String asJson = "{\"type\":\"geo\",\"regions\":{\"US\":[\"US-VA\",\"US-CA\"],\"IM\":[\"IM\"]}}";

    public void serializeNaturallyAsJson() throws JsonProcessingException {
        assertEquals(new ObjectMapper().writeValueAsString(geo), asJson);
    }

    public void equalToDeserializedMap() throws IOException {
        assertEquals(new ObjectMapper().readValue(asJson, new TypeReference<Map<String, Object>>() {
        }), geo);
    }

    Map<String, Object> geoWhereRegionsAreMapStringCollectionString = ImmutableMap.<String, Object> builder()//
            .put("type", "geo")//
            .put("regions", ImmutableMultimap.<String, String> builder()//
                    .put("US", "US-VA")//
                    .put("US", "US-CA")//
                    .put("IM", "IM").build().asMap()).build();

    public void asGeo() {
        assertEquals(Geo.asGeo(geoWhereRegionsAreMapStringCollectionString), geo);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "expected profile to have a Map<String, Collection<String>> regions field, not class com.google.common.collect.ImmutableListMultimap")
    public void asGeoNoGuavaAllowed() {
        Map<String, Object> geoWhereRegionsAreMultimapStringString = ImmutableMap.<String, Object> builder()//
                .put("type", "geo")//
                .put("regions", ImmutableMultimap.<String, String> builder()//
                        .put("US", "US-VA")//
                        .put("US", "US-CA")//
                        .put("IM", "IM").build()).build();

        Geo.asGeo(geoWhereRegionsAreMultimapStringString);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "expected regions values to be a subtype of Iterable<String>, not String")
    public void asGeoMultivalueTerritories() {
        Map<String, Object> geoWhereRegionsAreMultimapStringString = ImmutableMap.<String, Object> builder()//
                .put("type", "geo")//
                .put("regions", ImmutableMap.<String, String> builder()//
                        .put("US", "US-VA")//
                        .put("IM", "IM").build()).build();

        Geo.asGeo(geoWhereRegionsAreMultimapStringString);
    }

    ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData> builder()//
            .name("www.denominator.io.")//
            .type("A")//
            .qualifier("US-East")//
            .ttl(3600)//
            .add(AData.create("1.1.1.1"))//
            .addProfile(geoWhereRegionsAreMapStringCollectionString).build();

    public void asGeoRRSet() {
        assertEquals(Geo.asGeo(geoRRS), geo);
    }
}
