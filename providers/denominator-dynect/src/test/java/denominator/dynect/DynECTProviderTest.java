package denominator.dynect;

import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Provider;

public class DynECTProviderTest {

    @Test
    public void testDynECTRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new DynECTProvider()));
    }

    @Test
    public void testProviderWiresDynECTZoneApi() {
        assertEquals(create(new DynECTProvider()).getApi().getZoneApi().getClass(), DynECTZoneApi.class);
        assertEquals(create("dynect").getApi().getZoneApi().getClass(), DynECTZoneApi.class);
    }
}
