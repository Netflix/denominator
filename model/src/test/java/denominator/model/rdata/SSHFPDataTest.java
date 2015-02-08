package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.sshfp;

public class SSHFPDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testGoodRecord() {
    sshfp("www.denominator.io.", "1 1 B33F");
  }

  @Test
  public void testMissingParts() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("record must have exactly three parts");

    sshfp("www.denominator.io.", "1");
  }
}
