package denominator.mock;

import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Provider;

public class MockProviderTest {

    @Test
    public void testMockRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new MockProvider()));
    }

    @Test
    public void testProviderWiresMockZoneApi() {
        assertEquals(create(new MockProvider()).getApi().getZoneApi().getClass(), MockZoneApi.class);
        assertEquals(create("mock").getApi().getZoneApi().getClass(), MockZoneApi.class);
    }
}
