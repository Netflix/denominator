package denominator.model.profile;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSetsTest;
import denominator.model.rdata.AData;

import static org.testng.Assert.assertEquals;

@Test
public class WeightedTest {

  static Weighted weighted = Weighted.create(2);
  static ResourceRecordSet<AData> weightedRRS = ResourceRecordSet.<AData>builder()//
      .name("www.denominator.io.")//
      .type("A")//
      .qualifier("US-East")//
      .ttl(3600)//
      .add(AData.create("1.1.1.1"))//
      .weighted(Weighted.create(2)).build();
  String asJson = "{\"weight\":2}";

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "weight must be positive")
  public void testInvalidWeight() {
    Weighted.create(-1);
  }

  public void serializeNaturallyAsJson() {
    assertEquals(ResourceRecordSetsTest.gson.toJson(weighted), asJson);
  }

  public void deserializesNaturallyFromJson() throws IOException {
    assertEquals(ResourceRecordSetsTest.gson.fromJson(asJson, Weighted.class), weighted);
  }
}
