package denominator.ultradns;

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

public class UltraDNSProviderTest {

    @Test
    public void testUltraDNSRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(new UltraDNSProvider()));
    }

    @Test
    public void testProviderWiresUltraDNSZoneApi() {
        DNSApiManager manager = create(new UltraDNSProvider(), staticCredentials("username", "password"));
        assertEquals(manager.getApi().getZoneApi().getClass(), UltraDNSZoneApi.class);
        manager = create("ultradns", staticCredentials("username", "password"));
        assertEquals(manager.getApi().getZoneApi().getClass(), UltraDNSZoneApi.class);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "ultradns requires credentials with two parts: username and password")
    public void testCredentialsRequired() {
        create(new UltraDNSProvider()).getApi().getZoneApi().list();
    }
}
