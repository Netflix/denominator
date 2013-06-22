package denominator.ultradns;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import denominator.ultradns.UltraDNS.Record;

@Test
public class UltraDNSPredicatesTest {
    Record a;

    UltraDNSPredicatesTest() {
        a = new Record();
        a.id = "AAAAAAAAAAAA";
        a.name = "foo.com.";
        a.typeCode = 1;
        a.ttl = 3600;
        a.rdata.add("192.0.2.1");
    }

    public void resourceTypeEqualToFalseOnDifferentType() {
        assertFalse(UltraDNSPredicates.resourceTypeEqualTo(28).apply(a));
    }

    public void resourceTypeEqualToTrueOnSameType() {
        assertTrue(UltraDNSPredicates.resourceTypeEqualTo(a.typeCode).apply(a));
    }

    public void recordIdEqualToFalseOnDifferentId() {
        assertFalse(UltraDNSPredicates.recordIdEqualTo("BBBBBBBBBBBB").apply(a));
    }

    public void recordIdEqualToTrueOnSameId() {
        assertTrue(UltraDNSPredicates.recordIdEqualTo(a.id).apply(a));
    }
}
