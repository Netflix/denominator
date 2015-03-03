package denominator.ultradns;

import org.junit.Test;

import denominator.ultradns.UltraDNS.Record;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UltraDNSPredicatesTest {

  Record a;

  public UltraDNSPredicatesTest() {
    a = new Record();
    a.id = "AAAAAAAAAAAA";
    a.name = "foo.com.";
    a.typeCode = 1;
    a.ttl = 3600;
    a.rdata.add("192.0.2.1");
  }

  @Test
  public void resourceTypeEqualToFalseOnDifferentType() {
    assertFalse(UltraDNSFilters.resourceTypeEqualTo(28).apply(a));
  }

  @Test
  public void resourceTypeEqualToTrueOnSameType() {
    assertTrue(UltraDNSFilters.resourceTypeEqualTo(a.typeCode).apply(a));
  }

  @Test
  public void recordIdEqualToFalseOnDifferentId() {
    assertFalse(UltraDNSFilters.recordIdEqualTo("BBBBBBBBBBBB").apply(a));
  }

  @Test
  public void recordIdEqualToTrueOnSameId() {
    assertTrue(UltraDNSFilters.recordIdEqualTo(a.id).apply(a));
  }
}
