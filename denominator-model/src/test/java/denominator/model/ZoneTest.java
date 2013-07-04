package denominator.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

@Test
public class ZoneTest {
    public void factoryMethodsWork() {
        Zone name = Zone.create("denominator.io.");

        assertEquals(name.name(), "denominator.io.");
        assertNull(name.id());
        assertEquals(name, new Zone("denominator.io.", null));
        assertEquals(name.hashCode(), new Zone("denominator.io.", null).hashCode());
        assertEquals(name.toString(), "Zone{name=denominator.io.}");

        Zone id = Zone.create("denominator.io.", "ABCD");

        assertEquals(id.name(), "denominator.io.");
        assertEquals(id.id(), "ABCD");
        assertEquals(id, new Zone("denominator.io.", "ABCD"));
        assertEquals(id.hashCode(), new Zone("denominator.io.", "ABCD").hashCode());
        assertEquals(id.toString(), "Zone{name=denominator.io., id=ABCD}");

        assertNotEquals(name, id);
        assertNotEquals(name.hashCode(), id.hashCode());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "name")
    public void nullNameNPEMessage() {
        new Zone(null, "id");
    }
}
