package denominator.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

@Test
public class ZonesTest {

    Zone name = Zone.create("denominator.io.");

    public void nameEqualToReturnsFalseOnNull() {
        assertFalse(Zones.nameEqualTo(name.name()).apply(null));
    }

    public void nameEqualToReturnsFalseOnDifferentName() {
        assertFalse(Zones.nameEqualTo("www.foo.com").apply(name));
    }

    public void nameEqualToReturnsTrueOnSameName() {
        assertTrue(Zones.nameEqualTo(name.name()).apply(name));
    }

    Zone id = Zone.create("denominator.io.", "ABCD");

    public void idEqualToReturnsFalseOnNull() {
        assertFalse(Zones.idEqualTo(id.id().get()).apply(null));
    }

    public void idEqualToReturnsFalseOnDifferentId() {
        assertFalse(Zones.idEqualTo("EFGH").apply(id));
    }

    public void idEqualToReturnsFalseOnAbsentId() {
        assertFalse(Zones.idEqualTo(id.id().get()).apply(name));
    }

    public void idEqualToReturnsTrueOnSameId() {
        assertTrue(Zones.idEqualTo(id.id().get()).apply(id));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "zone")
    public void nullNameNPEMessageOnToName() {
        Zones.toName().apply(null);
    }

    public void toNameReturnsName() {
        assertEquals(Zones.toName().apply(name), name.name());
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "name")
    public void nullNameNPEMessageOnToZone() {
        Zones.toZone().apply(null);
    }

    public void toZoneReturnsZone() {
        assertEquals(Zones.toZone().apply(name.name()), name);
    }
}
