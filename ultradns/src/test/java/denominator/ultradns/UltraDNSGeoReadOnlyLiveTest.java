package denominator.ultradns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseGeoReadOnlyLiveTest;

@Test
public class UltraDNSGeoReadOnlyLiveTest extends BaseGeoReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new UltraDNSConnection().manager;
    }
}
