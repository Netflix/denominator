package denominator.stub;

import static denominator.Denominator.connectToBackend;
import static denominator.Denominator.listBackends;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Backend;

public class StubBackendTest {

    @Test
    public void testStubRegistered() {
        Set<Backend> allBackends = ImmutableSet.copyOf(listBackends());
        assertTrue(allBackends.contains(new StubBackend()));
    }

    @Test
    public void testBackendWiresStubZoneApi() {
        assertEquals(connectToBackend(new StubBackend()).open().getZoneApi().getClass(), StubZoneApi.class);
        assertEquals(connectToBackend("stub").open().getZoneApi().getClass(), StubZoneApi.class);
    }
}
