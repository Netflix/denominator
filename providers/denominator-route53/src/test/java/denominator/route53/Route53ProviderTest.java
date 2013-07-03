package denominator.route53;

import static denominator.CredentialsConfiguration.anonymous;
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

public class Route53ProviderTest {
    private static final Provider PROVIDER = new Route53Provider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.name(), "route53");
        assertEquals(PROVIDER.supportsDuplicateZoneNames(), true);
        assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMultimap.<String, String> builder()
                .putAll("accessKey", "accessKey", "secretKey")
                .putAll("session", "accessKey", "secretKey", "sessionToken").build().asMap());
    }

    @Test
    public void testRoute53Registered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(providers());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresRoute53ZoneApiWithAccessKeyCredentials() {
        DNSApiManager manager = create(PROVIDER, credentials("accesskey", "secretkey"));
        assertEquals(manager.api().zones().getClass(), Route53ZoneApi.class);
        manager = create("route53", credentials("accesskey", "secretkey"));
        assertEquals(manager.api().zones().getClass(), Route53ZoneApi.class);
        manager = create("route53", credentials(MapCredentials.from(ImmutableMap.<String, String> builder()
                                                              .put("accesskey", "A")
                                                              .put("secretkey", "S").build())));
        assertEquals(manager.api().zones().getClass(), Route53ZoneApi.class);
    }

    @Test
    public void testProviderWiresRoute53ZoneApiWithSessionCredentials() {
        DNSApiManager manager = create(PROVIDER, credentials("accesskey", "secretkey", "sessionToken"));
        assertEquals(manager.api().zones().getClass(), Route53ZoneApi.class);
        manager = create("route53", credentials("accesskey", "secretkey", "sessionToken"));
        assertEquals(manager.api().zones().getClass(), Route53ZoneApi.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. route53 requires one of the following forms: when type is accessKey: accessKey,secretKey; session: accessKey,secretKey,sessionToken")
    public void testCredentialsRequired() {
        // manually passing anonymous in case this test is executed from EC2
        // where IAM profiles are present.
        create(PROVIDER, anonymous()).api().zones().iterator();
    }

    @Test
    public void testViaDagger() {
        DNSApiManager manager = ObjectGraph
                .create(provider(new Route53Provider()), new Route53Provider.Module(), credentials("accesskey", "secretkey"))
                .get(DNSApiManager.class);
        assertEquals(manager.api().zones().getClass(), Route53ZoneApi.class);
    }
}
