package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.ns;

public class NSDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testNullTargetNS() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("record");

    ns("www.denominator.io.", (String) null);
  }
}
