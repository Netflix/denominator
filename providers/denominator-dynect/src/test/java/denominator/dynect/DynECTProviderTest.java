package denominator.dynect;

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

public class DynECTProviderTest {

    @Test
    public void testDynECTRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new DynECTProvider()));
    }

    @Test
    public void testProviderWiresDynECTZoneApi() {
        DNSApiManager manager = create(new DynECTProvider(), staticCredentials("customer", "username", "password"));
        assertEquals(manager.getApi().getZoneApi().getClass(), DynECTZoneApi.class);
        manager = create("dynect", staticCredentials("customer", "username", "password"));
        assertEquals(manager.getApi().getZoneApi().getClass(), DynECTZoneApi.class);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "dynect requires credentials with three parts: customer, username, password")
    public void testCredentialsRequired() {
        create(new DynECTProvider()).getApi().getZoneApi().list();
    }
}
