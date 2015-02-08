package denominator.model.profile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WeightedTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testInvalidWeight() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("weight must be positive");

    Weighted.create(-1);
  }
}
