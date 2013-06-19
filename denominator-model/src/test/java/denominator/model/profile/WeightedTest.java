package denominator.model.profile;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

@Test
public class WeightedTest {
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "weight must be positive")
    public void testInvalidWeight() {
        Weighted.create(-1);
    }

    Weighted weighted = Weighted.create(2);

    String asJson = "{\"type\":\"weighted\",\"weight\":2}";

    public void serializeNaturallyAsJson() throws JsonProcessingException {
        assertEquals(new ObjectMapper().writeValueAsString(weighted), asJson);
    }

    public void equalToDeserializedMap() throws IOException {
        assertEquals(new ObjectMapper().readValue(asJson, new TypeReference<Map<String, Object>>() {
        }), weighted);
    }

    Map<String, Object> weightedMap = ImmutableMap.<String, Object> builder()//
            .put("type", "weighted")//
            .put("weight", 2).build();

    public void asWeighted() {
        assertEquals(Weighted.asWeighted(weightedMap), weighted);
    }

    ResourceRecordSet<AData> weightedRRS = ResourceRecordSet.<AData> builder()//
            .name("www.denominator.io.")//
            .type("A")//
            .qualifier("US-East")//
            .ttl(3600)//
            .add(AData.create("1.1.1.1"))//
            .addProfile(weightedMap).build();

    public void asWeightedRRSet() {
        assertEquals(Weighted.asWeighted(weightedRRS), weighted);
    }
}
