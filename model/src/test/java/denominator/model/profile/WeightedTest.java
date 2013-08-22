package denominator.model.profile;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

@Test
public class WeightedTest {
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "weight must be positive")
    public void testInvalidWeight() {
        Weighted.create(-1);
    }

    static Weighted weighted = Weighted.create(2);

    String asJson = "{\"weight\":2}";

    public void serializeNaturallyAsJson() throws JsonProcessingException {
        assertEquals(new ObjectMapper().writeValueAsString(weighted), asJson);
    }

    public void equalToDeserializedMap() throws IOException {
        assertEquals(new ObjectMapper().readValue(asJson, Map.class), weighted);
    }

    static ResourceRecordSet<AData> weightedRRS = ResourceRecordSet.<AData> builder()//
            .name("www.denominator.io.")//
            .type("A")//
            .qualifier("US-East")//
            .ttl(3600)//
            .add(AData.create("1.1.1.1"))//
            .weighted(Weighted.create(2)).build();
}
