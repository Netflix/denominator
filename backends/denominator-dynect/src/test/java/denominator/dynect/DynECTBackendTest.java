package denominator.dynect;

import static denominator.Denominator.connectToBackend;
import static denominator.Denominator.listBackends;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Backend;

public class DynECTBackendTest {

    @Test
    public void testDynECTRegistered() {
        Set<Backend> allBackends = ImmutableSet.copyOf(listBackends());
        assertTrue(allBackends.contains(new DynECTBackend()));
    }

    @Test
    public void testBackendWiresDynECTZoneApi() {
        assertEquals(connectToBackend(new DynECTBackend()).open().getZoneApi().getClass(), DynECTZoneApi.class);
        assertEquals(connectToBackend("dynect").open().getZoneApi().getClass(), DynECTZoneApi.class);
    }
}
