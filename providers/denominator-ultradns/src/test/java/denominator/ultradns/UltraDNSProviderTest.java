package denominator.ultradns;

import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Provider;

public class UltraDNSProviderTest {

    @Test
    public void testUltraDNSRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new UltraDNSProvider()));
    }

    @Test
    public void testProviderWiresUltraDNSZoneApi() {
        assertEquals(create(new UltraDNSProvider()).getApi().getZoneApi().getClass(), UltraDNSZoneApi.class);
        assertEquals(create("ultradns").getApi().getZoneApi().getClass(), UltraDNSZoneApi.class);
    }
}
