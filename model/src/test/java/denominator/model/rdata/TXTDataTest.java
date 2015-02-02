package denominator.model.rdata;

import org.testng.annotations.Test;

import static denominator.model.ResourceRecordSets.txt;

@Test
public class TXTDataTest {

  public void testSinglePart() {
    txt("www.denominator.io.", "foo");
  }

  public void testMultiPart() {
    txt("www.denominator.io.", "\"foo bar\"");
  }

}
