package denominator.ultradns;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.profile.BaseGeoWriteCommandsLiveTest;

@Test
public class UltraDNSGeoWriteCommandsLiveTest extends BaseGeoWriteCommandsLiveTest {
    @BeforeClass
    private void setUp() {
        UltraDNSConnection connection = new UltraDNSConnection();
        manager = connection.manager;
        mutableGeoRRSet = connection.mutableGeoRRSet;
    }
}
