package denominator.dynect;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Denominator.providers;
import static denominator.Denominator.provider;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import dagger.ObjectGraph;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.Credentials.MapCredentials;

public class DynECTProviderTest {
    private static final Provider PROVIDER = new DynECTProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.name(), "dynect");
        assertEquals(PROVIDER.supportsDuplicateZoneNames(), false);
        assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("password", "customer", "username", "password").build());
    }

    @Test
    public void testDynECTRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(providers());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresDynECTZoneApi() {
        DNSApiManager manager = create(PROVIDER, credentials("customer", "username", "password"));
        assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
        manager = create("dynect", credentials("customer", "username", "password"));
        assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
        manager = create("dynect", credentials(MapCredentials.from(ImmutableMap.<String, String> builder()
                                                             .put("customer", "C")
                                                             .put("username", "U")
                                                             .put("password", "P").build())));
        assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. dynect requires customer, username, password")
    public void testCredentialsRequired() {
        create(PROVIDER).api().zones().iterator();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. dynect requires customer, username, password")
    public void testThreePartCredentialsRequired() {
        create(PROVIDER, credentials("username", "password")).api().zones().iterator();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(provider(new DynECTProvider()), new DynECTProvider.Module(), credentials("customer", "username", "password"))
                .get(DNSApiManager.class);
        assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
    }
}
