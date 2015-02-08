package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.ds;

public class DSDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGoodRecord() {
    ds("www.denominator.io.", "12345 1 1 B33F");
  }

  @Test
  public void testMissingParts() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("record must have exactly four parts");

    ds("www.denominator.io.", "12345 1 1");
  }
}
