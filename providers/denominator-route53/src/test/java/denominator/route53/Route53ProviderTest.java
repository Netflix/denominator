package denominator.route53;

import static denominator.CredentialsConfiguration.staticCredentials;
import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.DNSApiManager;
import denominator.Provider;

public class Route53ProviderTest {

    @Test
    public void testRoute53Registered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new Route53Provider()));
    }

    @Test
    public void testProviderWiresRoute53ZoneApi() {
        DNSApiManager manager = create(new Route53Provider(), staticCredentials("accesskey", "secretkey"));
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
        manager = create("route53", staticCredentials("accesskey", "secretkey"));
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "route53 requires credentials with two parts: accessKey and secretKey")
    public void testCredentialsRequired() {
        create(new Route53Provider()).getApi().getZoneApi().list();
    }
}
