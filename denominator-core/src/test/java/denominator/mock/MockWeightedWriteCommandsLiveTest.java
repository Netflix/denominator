package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseWeightedWriteCommandsLiveTest;

@Test
public class MockWeightedWriteCommandsLiveTest extends BaseWeightedWriteCommandsLiveTest {
    @BeforeClass
    private void setUp() {
        MockConnection connection = new MockConnection();
        manager = connection.manager;
        mutableZone = manager.api().zones().iterator().next();
    }
}
