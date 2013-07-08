package denominator.designate;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRoundRobinLiveTest;

@Test
public class DesignateRoundRobinLiveTest extends BaseRoundRobinLiveTest {
    @BeforeClass
    private void setUp() {
        DesignateConnection connection = new DesignateConnection();
        manager = connection.manager;
        setMutableZoneIfPresent(connection.mutableZone);
    }
}
