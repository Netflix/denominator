package denominator.ultradns;

import org.testng.annotations.Test;

import denominator.ultradns.UltraDNS.Record;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
    assertFalse(UltraDNSFilters.resourceTypeEqualTo(28).apply(a));
  }

  public void resourceTypeEqualToTrueOnSameType() {
    assertTrue(UltraDNSFilters.resourceTypeEqualTo(a.typeCode).apply(a));
  }

  public void recordIdEqualToFalseOnDifferentId() {
    assertFalse(UltraDNSFilters.recordIdEqualTo("BBBBBBBBBBBB").apply(a));
  }

  public void recordIdEqualToTrueOnSameId() {
    assertTrue(UltraDNSFilters.recordIdEqualTo(a.id).apply(a));
  }
}
