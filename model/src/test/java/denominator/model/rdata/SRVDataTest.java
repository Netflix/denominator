package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.srv;

public class SRVDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGoodRecord() {
    srv("www.denominator.io.", "0 1 80 www.foo.com.");
  }

  @Test
  public void testMissingParts() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("record must have exactly four parts");

    srv("www.denominator.io.", "0 1 80");
  }
}
