package denominator.model.rdata;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.a;

public class ADataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testBadIPv6() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("should be a ipv4 address");

    a("www.denominator.io.", "2001:db8:1cfe:face:b00c::3");
  }

  @Test
  public void testNoIP() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("should be a ipv4 address");

    a("www.denominator.io.", "");
  }

  @Test
  public void testGoodIPv4() {
    a("www.denominator.io.", "192.0.2.1");
  }
}
