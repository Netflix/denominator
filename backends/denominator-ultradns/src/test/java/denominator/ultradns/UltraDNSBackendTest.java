package denominator.ultradns;

import static denominator.Denominator.connectToBackend;
import static denominator.Denominator.listBackends;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Backend;

public class UltraDNSBackendTest {

    @Test
    public void testUltraDNSRegistered() {
        Set<Backend> allBackends = ImmutableSet.copyOf(listBackends());
        assertTrue(allBackends.contains(new UltraDNSBackend()));
    }

    @Test
    public void testBackendWiresUltraDNSZoneApi() {
        assertEquals(connectToBackend(new UltraDNSBackend()).open().getZoneApi().getClass(), UltraDNSZoneApi.class);
        assertEquals(connectToBackend("ultradns").open().getZoneApi().getClass(), UltraDNSZoneApi.class);
    }
}
