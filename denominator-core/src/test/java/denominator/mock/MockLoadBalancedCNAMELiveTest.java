package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseLoadBalancedCNAMELiveTest;

@Test
public class MockLoadBalancedCNAMELiveTest extends BaseLoadBalancedCNAMELiveTest {
    @BeforeClass
    private void setUp() {
        MockConnection connection = new MockConnection();
        manager = connection.manager;
        mutableZone = connection.mutableZone;
    }
}
