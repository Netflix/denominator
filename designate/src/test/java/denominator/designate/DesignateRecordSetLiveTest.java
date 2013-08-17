package denominator.designate;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRecordSetLiveTest;

@Test
public class DesignateRecordSetLiveTest extends BaseRecordSetLiveTest {
    @BeforeClass
    private void setUp() {
        DesignateConnection connection = new DesignateConnection();
        manager = connection.manager;
        setMutableZoneIfPresent(connection.mutableZone);
    }
}
