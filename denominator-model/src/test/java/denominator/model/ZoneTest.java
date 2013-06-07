package denominator.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

import org.testng.annotations.Test;

import com.google.common.base.Optional;

@Test
public class ZoneTest {
    public void factoryMethodsWork() {
        Zone name = Zone.create("denominator.io.");

        assertEquals(name.name(), "denominator.io.");
        assertFalse(name.id().isPresent());
        assertEquals(name, new Zone("denominator.io.", Optional.<String> absent()));
        assertEquals(name.hashCode(), new Zone("denominator.io.", Optional.<String> absent()).hashCode());
        assertEquals(name.toString(), "Zone{name=denominator.io.}");

        Zone id = Zone.create("denominator.io.", "ABCD");

        assertEquals(id.name(), "denominator.io.");
        assertEquals(id.id().get(), "ABCD");
        assertEquals(id, new Zone("denominator.io.", Optional.of("ABCD")));
        assertEquals(id.hashCode(), new Zone("denominator.io.", Optional.of("ABCD")).hashCode());
        assertEquals(id.toString(), "Zone{name=denominator.io., id=ABCD}");

        assertNotEquals(name, id);
        assertNotEquals(name.hashCode(), id.hashCode());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "name")
    public void nullNameNPEMessage() {
        new Zone(null, Optional.of("id"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "id of denominator.io.")
    public void nullIdNPEMessage() {
        new Zone("denominator.io.", null);
    }
}
