package denominator.ultradns;

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
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Provider;

public class UltraDNSProviderTest {
    private static final Provider PROVIDER = new UltraDNSProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.name(), "ultradns");
        assertEquals(PROVIDER.supportsDuplicateZoneNames(), false);
        assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("password", "username", "password").build());
    }

    @Test
    public void testUltraDNSRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(providers());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresUltraDNSZoneApi() {
        DNSApiManager manager = create(PROVIDER, credentials("username", "password"));
        assertEquals(manager.api().zones().getClass(), UltraDNSZoneApi.class);
        manager = create("ultradns", credentials("username", "password"));
        assertEquals(manager.api().zones().getClass(), UltraDNSZoneApi.class);
        manager = create("ultradns", credentials(MapCredentials.from(ImmutableMap.<String, String> builder()
                                                               .put("username", "U")
                                                               .put("password", "P").build())));
        assertEquals(manager.api().zones().getClass(), UltraDNSZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. ultradns requires username, password")
    public void testCredentialsRequired() {
        create(PROVIDER).api().zones().iterator();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. ultradns requires username, password")
    public void testTwoPartCredentialsRequired() {
        create(PROVIDER, credentials("customer", "username", "password")).api().zones().iterator();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(provider(new UltraDNSProvider()), new UltraDNSProvider.Module(), credentials("username", "password"))
                .get(DNSApiManager.class);
        assertEquals(manager.api().zones().getClass(), UltraDNSZoneApi.class);
    }
}
