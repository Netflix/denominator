package denominator.clouddns;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import dagger.ObjectGraph;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Provider;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Providers.list;
import static denominator.Providers.provide;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

public class CloudDNSProviderTest {

  private static final Provider PROVIDER = new CloudDNSProvider();

  @Test
  public void testMockMetadata() {
    assertEquals(PROVIDER.name(), "clouddns");
    assertEquals(PROVIDER.supportsDuplicateZoneNames(), true);
    assertThat(PROVIDER.credentialTypeToParameterNames())
        .containsEntry("password", Arrays.asList("username", "password"))
        .containsEntry("apiKey", Arrays.asList("username", "apiKey"));
  }

  @Test
  public void testCloudDNSRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresCloudDNSZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("username", "apiKey"));
    assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
    manager = create("clouddns", credentials("username", "apiKey"));
    assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("username", "U");
    map.put("apiKey", "K");
    manager = create("clouddns", credentials(MapCredentials.from(map)));
    assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. clouddns requires one of the following forms: when type is password: username,password; apiKey: username,apiKey")
  public void testCredentialsRequired() {
    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new CloudDNSProvider()), new CloudDNSProvider.Module(),
                credentials("username", "apiKey"))
        .get(DNSApiManager.class);
    assertEquals(manager.api().zones().getClass(), CloudDNSZoneApi.class);
  }
}
