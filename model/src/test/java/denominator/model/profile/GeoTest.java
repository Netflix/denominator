package denominator.model.profile;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;

@Test
public class GeoTest {

    static Geo geo = Geo.create(ImmutableMultimap.<String, String> builder()//
            .put("US", "US-VA")//
            .put("US", "US-CA")//
            .put("IM", "IM").build().asMap());

    String asJson = "{\"regions\":{\"US\":[\"US-VA\",\"US-CA\"],\"IM\":[\"IM\"]}}";

    public void serializeNaturallyAsJson() throws JsonProcessingException {
        assertEquals(new ObjectMapper().writeValueAsString(geo), asJson);
    }

    public void equalToDeserializedMap() throws IOException {
        assertEquals(new ObjectMapper().readValue(asJson, Map.class), geo);
    }
}
