package denominator.model.rdata;

import org.junit.Test;

import static denominator.model.ResourceRecordSets.txt;

public class TXTDataTest {

  @Test
  public void testSinglePart() {
    txt("www.denominator.io.", "foo");
  }

  @Test
  public void testMultiPart() {
    txt("www.denominator.io.", "\"foo bar\"");
  }
}
