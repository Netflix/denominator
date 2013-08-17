package denominator.designate;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Denominator.provider;
import static denominator.Denominator.providers;
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

public class DesignateProviderTest {
    private static final Provider PROVIDER = new DesignateProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.name(), "designate");
        assertEquals(PROVIDER.supportsDuplicateZoneNames(), true);
        assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("password", "tenantId", "username", "password").build().asMap());
    }

    @Test
    public void testDesignateRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(providers());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresDesignateZoneApi() {
        DNSApiManager manager = create(PROVIDER, credentials("tenantId", "username", "password"));
        assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
        manager = create("designate", credentials("tenantId", "username", "password"));
        assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
        manager = create("designate", credentials(MapCredentials.from(ImmutableMap.<String, String> builder()
                                                                .put("tenantId", "T")
                                                                .put("username", "U")
                                                                .put("password", "P").build())));
        assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. designate requires tenantId,username,password")
    public void testCredentialsRequired() {
        create(PROVIDER).api().zones().iterator();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(provider(new DesignateProvider()), new DesignateProvider.Module(), credentials("tenantId", "username", "password"))
                .get(DNSApiManager.class);
        assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
    }
}
