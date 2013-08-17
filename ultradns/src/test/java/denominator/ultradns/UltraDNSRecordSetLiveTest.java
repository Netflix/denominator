package denominator.ultradns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseRecordSetLiveTest;

@Test
public class UltraDNSRecordSetLiveTest extends BaseRecordSetLiveTest {
    @BeforeClass
    private void setUp() {
        UltraDNSConnection connection = new UltraDNSConnection();
        manager = connection.manager;
        setMutableZoneIfPresent(connection.mutableZone);
    }
}
