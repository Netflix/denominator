package denominator.model.rdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static denominator.model.ResourceRecordSets.spf;

public class SPFDataTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testSinglePart() {
    spf("www.denominator.io.", "v=spf1 a mx -all");
  }

  @Test
  public void testMultiPart() {
    spf("www.denominator.io.", "\"v=spf1 a mx -all\" \"v=spf1 aaaa mx -all\"");
  }
}
