package denominator.model.profile;

import org.testng.annotations.Test;

@Test
public class WeightedTest {
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "weight must be positive")
    public void testInvalidWeight() {
        Weighted.create(-1);
    }
}
