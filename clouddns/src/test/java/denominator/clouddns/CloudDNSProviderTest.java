package denominator.clouddns;

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

public class CloudDNSProviderTest {
    private static final Provider PROVIDER = new CloudDNSProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.name(), "clouddns");
        assertEquals(PROVIDER.supportsDuplicateZoneNames(), true);
        assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("password", "username", "password")
                .putAll("apiKey", "username", "apiKey").build().asMap());
    }

    @Test
    public void testCloudDNSRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(providers());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresCloudDNSZoneApi() {
        DNSApiManager manager = create(PROVIDER, credentials("username", "apiKey"));
        assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
        manager = create("clouddns", credentials("username", "apiKey"));
        assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
        manager = create("clouddns", credentials(MapCredentials.from(ImmutableMap.<String, String> builder()
                                                               .put("username", "U")
                                                               .put("apiKey", "K").build())));
        assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. clouddns requires one of the following forms: when type is password: username,password; apiKey: username,apiKey")
    public void testCredentialsRequired() {
        create(PROVIDER).api().zones().iterator();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(provider(new CloudDNSProvider()), new CloudDNSProvider.Module(), credentials("username", "apiKey"))
                .get(DNSApiManager.class);
        assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
    }
}
