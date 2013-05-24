package denominator.clouddns;

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

public class CloudDNSProviderTest {
    private static final Provider PROVIDER = new CloudDNSProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.getName(), "clouddns");
        assertEquals(PROVIDER.getCredentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("apiKey", "username", "apiKey").build());
    }

    @Test
    public void testCloudDNSRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresCloudDNSZoneApi() {
        DNSApiManager manager = create(PROVIDER, credentials("username", "apiKey"));
        assertEquals(manager.getApi().getZoneApi().getClass(), CloudDNSZoneApi.class);
        manager = create("clouddns", credentials("username", "apiKey"));
        assertEquals(manager.getApi().getZoneApi().getClass(), CloudDNSZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. clouddns requires username, apiKey")
    public void testCredentialsRequired() {
        create(PROVIDER).getApi().getZoneApi().list();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(new CloudDNSProvider.Module(), credentials("username", "apiKey"))
                .get(DNSApiManager.class);
        assertEquals(manager.getApi().getZoneApi().getClass(), CloudDNSZoneApi.class);
    }
}
