package denominator.model.profile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class GeoTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void immutableRegions() {
    thrown.expect(UnsupportedOperationException.class);

    Map<String, Collection<String>> regions = new LinkedHashMap<String, Collection<String>>();
    regions.put("US", Arrays.asList("US-VA", "US-CA"));

    Geo geo = Geo.create(regions);

    geo.regions().remove("US");
  }
}
