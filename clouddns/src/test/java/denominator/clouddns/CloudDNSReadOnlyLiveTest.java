package denominator.clouddns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseReadOnlyLiveTest;

@Test
public class CloudDNSReadOnlyLiveTest extends BaseReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new CloudDNSConnection().manager;
    }
}
