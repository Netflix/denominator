package denominator.route53;

import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.Provider;

public class Route53ProviderTest {

    @Test
    public void testRoute53Registered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new Route53Provider()));
    }

    @Test
    public void testProviderWiresRoute53ZoneApi() {
        assertEquals(create(new Route53Provider()).getApi().getZoneApi().getClass(), Route53ZoneApi.class);
        assertEquals(create("route53").getApi().getZoneApi().getClass(), Route53ZoneApi.class);
    }
}
