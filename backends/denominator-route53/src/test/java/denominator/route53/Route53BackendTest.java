package denominator.route53;

import static denominator.Denominator.connectToBackend;
import static denominator.Denominator.listBackends;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Backend;

public class Route53BackendTest {

    @Test
    public void testRoute53Registered() {
        Set<Backend> allBackends = ImmutableSet.copyOf(listBackends());
        assertTrue(allBackends.contains(new Route53Backend()));
    }

    @Test
    public void testBackendWiresRoute53ZoneApi() {
        assertEquals(connectToBackend(new Route53Backend()).open().getZoneApi().getClass(), Route53ZoneApi.class);
        assertEquals(connectToBackend("route53").open().getZoneApi().getClass(), Route53ZoneApi.class);
    }
}
