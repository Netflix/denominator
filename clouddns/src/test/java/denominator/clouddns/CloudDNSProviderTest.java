package denominator.clouddns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

public class CloudDNSProviderTest {

  private static final Provider PROVIDER = new CloudDNSProvider();

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void testCloudDNSMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("clouddns");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
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
    assertThat(manager.api().zones()).isInstanceOf(CloudDNSZoneApi.class);
    manager = create("clouddns", credentials("username", "apiKey"));
    assertThat(manager.api().zones()).isInstanceOf(CloudDNSZoneApi.class);
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("username", "U");
    map.put("apiKey", "K");
    manager = create("clouddns", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(CloudDNSZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "no credentials supplied. clouddns requires one of the following forms: when type is password: username,password; apiKey: username,apiKey");

    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new CloudDNSProvider()), new CloudDNSProvider.Module(),
                credentials("username", "apiKey"))
        .get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(CloudDNSZoneApi.class);
  }
}
