package denominator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import denominator.ResourceTypeToValue.ResourceTypes;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceTypeToValueTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testNiceExceptionOnNotFound() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "ResourceTypes do not include RRRR; types: " + Arrays.asList(ResourceTypes.values()));

    ResourceTypeToValue.lookup("RRRR");
  }

  @Test
  public void testNiceExceptionOnNull() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("resource type was null");

    ResourceTypeToValue.lookup((String) null);
  }

  @Test
  public void testBasicCase() {

    assertThat(ResourceTypeToValue.lookup("AAAA")).isEqualTo(28);
  }
}
