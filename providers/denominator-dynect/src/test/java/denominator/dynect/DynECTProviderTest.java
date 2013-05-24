package denominator.dynect;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import dagger.ObjectGraph;
import denominator.DNSApiManager;
import denominator.Provider;

public class DynECTProviderTest {
    private static final Provider PROVIDER = new DynECTProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.getName(), "dynect");
        assertEquals(PROVIDER.getCredentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("password", "customer", "username", "password").build());
    }

    @Test
    public void testDynECTRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresDynECTZoneApi() {
        DNSApiManager manager = create(PROVIDER, credentials("customer", "username", "password"));
        assertEquals(manager.getApi().getZoneApi().getClass(), DynECTZoneApi.class);
        manager = create("dynect", credentials("customer", "username", "password"));
        assertEquals(manager.getApi().getZoneApi().getClass(), DynECTZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. dynect requires customer, username, password")
    public void testCredentialsRequired() {
        create(PROVIDER).getApi().getZoneApi().list();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. dynect requires customer, username, password")
    public void testThreePartCredentialsRequired() {
        create(PROVIDER, credentials("username", "password")).getApi().getZoneApi().list();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(new DynECTProvider.Module(), credentials("customer", "username", "password"))
                .get(DNSApiManager.class);
        assertEquals(manager.getApi().getZoneApi().getClass(), DynECTZoneApi.class);
    }
}
