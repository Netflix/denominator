package denominator.route53;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseWeightedReadOnlyLiveTest;

@Test
public class Route53WeightedReadOnlyLiveTest extends BaseWeightedReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new Route53Connection().manager;
    }
}
