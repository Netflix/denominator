package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.aaaa;

public class AAAADataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testBadIPv4() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("should be a ipv6 address");

    aaaa("www.denominator.io.", "192.0.2.1");
  }

  @Test
  public void testNoIP() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("should be a ipv6 address");

    aaaa("www.denominator.io.", "");
  }

  @Test
  public void testGoodIPv6() {
    aaaa("www.denominator.io.", "2001:db8:1cfe:face:b00c::3");
  }
}
