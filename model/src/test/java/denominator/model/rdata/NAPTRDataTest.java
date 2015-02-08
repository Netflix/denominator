package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.naptr;

public class NAPTRDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGoodRecord() {
    naptr("www.denominator.io.", "1 1 U E2U+sip !^.*$!sip:customer-service@example.com! .");
  }

  @Test
  public void testMissingParts() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("record must have exactly six parts");

    naptr("www.denominator.io.", "1 1 U E2U+sip");
  }
}
