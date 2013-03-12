package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRecordSetLiveTest;

@Test
public class MockRecordSetLiveTest extends BaseRecordSetLiveTest {
    @BeforeClass
    private void setUp() {
        MockConnection connection = new MockConnection();
        manager = connection.manager;
        mutableZone = connection.mutableZone;
    }
}
