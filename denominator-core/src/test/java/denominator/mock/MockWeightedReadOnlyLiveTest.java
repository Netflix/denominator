package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseWeightedReadOnlyLiveTest;

@Test
public class MockWeightedReadOnlyLiveTest extends BaseWeightedReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new MockConnection().manager;
    }
}
