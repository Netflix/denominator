package denominator.model.profile;

import com.google.common.collect.ImmutableMultimap;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.model.ResourceRecordSetsTest;

import static org.testng.Assert.assertEquals;

@Test
public class GeoTest {

  static Geo geo = Geo.create(ImmutableMultimap.<String, String>builder()//
                                  .put("US", "US-VA")//
                                  .put("US", "US-CA")//
                                  .put("IM", "IM").build().asMap());

  String asJson = "{\"regions\":{\"US\":[\"US-VA\",\"US-CA\"],\"IM\":[\"IM\"]}}";

  public void serializeNaturallyAsJson() {
    assertEquals(ResourceRecordSetsTest.gson.toJson(geo), asJson);
  }

  public void deserializesNaturallyFromJson() throws IOException {
    assertEquals(ResourceRecordSetsTest.gson.fromJson(asJson, Geo.class), geo);
  }
}
