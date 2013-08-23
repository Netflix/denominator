package denominator.model.profile;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;

import denominator.model.ResourceRecordSetsTest;

@Test
public class GeoTest {

    static Geo geo = Geo.create(ImmutableMultimap.<String, String> builder()//
            .put("US", "US-VA")//
            .put("US", "US-CA")//
            .put("IM", "IM").build().asMap());

    String asJson = "{\"regions\":{\"US\":[\"US-VA\",\"US-CA\"],\"IM\":[\"IM\"]}}";

    public void serializeNaturallyAsJson() {
        assertEquals(ResourceRecordSetsTest.gson.toJson(geo), asJson);
    }

    public void deserializesNaturallyFromJson() throws IOException {
        assertEquals(ResourceRecordSetsTest.gson.fromJson(asJson, Geo.class), geo);
    }
}
