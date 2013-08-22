package denominator.model;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import denominator.model.rdata.AData;

@Test
public class ResourceRecordSetTest {
    ResourceRecordSet<AData> record = ResourceRecordSet.<AData> builder()
                                                       .name("www.denominator.io.")
                                                       .type("A")
                                                       .ttl(3600)
                                                       .add(AData.create("192.0.2.1")).build();

    String asJson = "{\"name\":\"www.denominator.io.\",\"type\":\"A\",\"ttl\":3600,\"records\":[{\"address\":\"192.0.2.1\"}]}";

    public void canBuildARecordSetInLongForm() {
        assertEquals(record.name(), "www.denominator.io.");
        assertEquals(record.type(), "A");
        assertEquals(record.ttl(), Integer.valueOf(3600));
        assertEquals(record.records().get(0), AData.create("192.0.2.1"));
    }

    public void serializeNaturallyAsJson() throws JsonProcessingException {
        assertEquals(new ObjectMapper().writeValueAsString(record), asJson);
    }

    public void equalToDeserializedMap() throws IOException {
        assertEquals(new ObjectMapper().readValue(asJson, Map.class), record);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "record")
    public void testNullRdataNPE() {
        ResourceRecordSet.<AData> builder().add(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Invalid ttl.*")
    public void testInvalidTTL() {
        ResourceRecordSet.<AData> builder()//
                .name("www.denominator.io.")//
                .type("A")//
                .ttl(0xFFFFFFFF)//
                .add(AData.create("192.0.2.1")).build();
    }
}
