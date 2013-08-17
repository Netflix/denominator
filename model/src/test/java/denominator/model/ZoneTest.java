package denominator.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Test
public class ZoneTest {
    public void factoryMethodsWork() {
        Zone name = Zone.create("denominator.io.");

        assertEquals(name.name(), "denominator.io.");
        assertNull(name.id());
        assertEquals(name, new Zone("denominator.io.", null));
        assertEquals(name.hashCode(), new Zone("denominator.io.", null).hashCode());
        assertEquals(name.toString(), "{name=denominator.io.}");

        Zone id = Zone.create("denominator.io.", "ABCD");

        assertEquals(id.name(), "denominator.io.");
        assertEquals(id.id(), "ABCD");
        assertEquals(id, new Zone("denominator.io.", "ABCD"));
        assertEquals(id.hashCode(), new Zone("denominator.io.", "ABCD").hashCode());
        assertEquals(id.toString(), "{name=denominator.io., id=ABCD}");

        assertNotEquals(name, id);
        assertNotEquals(name.hashCode(), id.hashCode());
    }

    public void serializeNaturallyAsJson() throws JsonProcessingException {
        assertEquals(new ObjectMapper().writeValueAsString(Zone.create("denominator.io.")),
                "{\"name\":\"denominator.io.\"}");
        assertEquals(new ObjectMapper().writeValueAsString(Zone.create("denominator.io.", "ABCD")),
                "{\"name\":\"denominator.io.\",\"id\":\"ABCD\"}");
    }

    public void equalToDeserializedMap() throws IOException {
        assertEquals(new ObjectMapper().readValue("{\"name\":\"denominator.io.\"}", Map.class),
                Zone.create("denominator.io."));
        assertEquals(new ObjectMapper().readValue("{\"name\":\"denominator.io.\",\"id\":\"ABCD\"}", Map.class),
                Zone.create("denominator.io.", "ABCD"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "name")
    public void nullNameNPEMessage() {
        new Zone(null, "id");
    }
}
