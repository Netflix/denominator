package denominator.dynect;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseGeoReadOnlyLiveTest;

@Test
public class DynECTGeoReadOnlyLiveTest extends BaseGeoReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new DynECTConnection().manager;
    }
}
