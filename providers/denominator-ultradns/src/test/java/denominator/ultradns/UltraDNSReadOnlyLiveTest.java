package denominator.ultradns;

import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;

import org.jclouds.ultradns.ws.UltraDNSWSResponseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseReadOnlyLiveTest;
import denominator.model.ResourceRecordSet;

@Test
public class UltraDNSReadOnlyLiveTest extends BaseReadOnlyLiveTest {
    @BeforeClass
    private void setUp() {
        manager = new UltraDNSConnection().manager;
    }

    @Override
    protected void checkListByNameAndTypeConsistent(String zoneName, ResourceRecordSet<?> rrs) {
        try {
            super.checkListByNameAndTypeConsistent(zoneName, rrs);
        } catch (UltraDNSWSResponseException e) {
            assertEquals(e.getError().getCode(), 2114);
            getAnonymousLogger().warning("invalid hostname in record set: " + rrs);
        }
    }
}
