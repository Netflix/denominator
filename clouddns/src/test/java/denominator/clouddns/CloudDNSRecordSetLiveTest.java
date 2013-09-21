package denominator.clouddns;

import denominator.BaseRecordSetLiveTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class CloudDNSRecordSetLiveTest extends BaseRecordSetLiveTest {
    @BeforeClass
    private void setUp() {
        CloudDNSConnection connection = new CloudDNSConnection();
        manager = connection.manager;
        setMutableZoneIfPresent(connection.mutableZone);
        addTrailingDotToZone = false;
    }
}
