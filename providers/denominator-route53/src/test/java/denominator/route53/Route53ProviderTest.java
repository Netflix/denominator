package denominator.route53;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static denominator.Denominator.provider;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import dagger.ObjectGraph;
import denominator.DNSApiManager;
import denominator.Provider;

public class Route53ProviderTest {
    private static final Provider PROVIDER = new Route53Provider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.getName(), "route53");
        assertEquals(PROVIDER.getCredentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("accessKey", "accessKey", "secretKey")
                .putAll("session", "accessKey", "secretKey", "sessionToken").build());
    }

    @Test
    public void testRoute53Registered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(listProviders());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresRoute53ZoneApiWithAccessKeyCredentials() {
        DNSApiManager manager = create(PROVIDER, credentials("accesskey", "secretkey"));
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
        manager = create("route53", credentials("accesskey", "secretkey"));
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
    }

    @Test
    public void testProviderWiresRoute53ZoneApiWithSessionCredentials() {
        DNSApiManager manager = create(PROVIDER, credentials("accesskey", "secretkey", "sessionToken"));
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
        manager = create("route53", credentials("accesskey", "secretkey", "sessionToken"));
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. route53 requires one of the following forms: when type is accessKey: accessKey, secretKey; session: accessKey, secretKey, sessionToken")
    public void testCredentialsRequired() {
        create(PROVIDER).getApi().getZoneApi().list();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(provider(new Route53Provider()), new Route53Provider.Module(), credentials("accesskey", "secretkey"))
                .get(DNSApiManager.class);
        assertEquals(manager.getApi().getZoneApi().getClass(), Route53ZoneApi.class);
    }
}
