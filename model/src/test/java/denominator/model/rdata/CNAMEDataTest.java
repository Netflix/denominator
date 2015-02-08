package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.cname;

public class CNAMEDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testNullTarget() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("record");

    cname("www.denominator.io.", (String) null);
  }
}
