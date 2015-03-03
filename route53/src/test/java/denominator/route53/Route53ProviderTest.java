package denominator.route53;

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

import static denominator.CredentialsConfiguration.anonymous;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Providers.list;
import static denominator.Providers.provide;
import static org.assertj.core.api.Assertions.assertThat;

public class Route53ProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static final Provider PROVIDER = new Route53Provider();

  @Test
  public void testRoute53Metadata() {
    assertThat(PROVIDER.name()).isEqualTo("route53");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isTrue();
    assertThat(PROVIDER.credentialTypeToParameterNames())
        .containsEntry("accessKey", Arrays.asList("accessKey", "secretKey"))
        .containsEntry("session", Arrays.asList("accessKey", "secretKey", "sessionToken"));
  }

  @Test
  public void testRoute53Registered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresRoute53ZoneApiWithAccessKeyCredentials() {
    DNSApiManager manager = create(PROVIDER, credentials("accesskey", "secretkey"));
    assertThat(manager.api().zones()).isInstanceOf(Route53ZoneApi.class);
    manager = create("route53", credentials("accesskey", "secretkey"));
    assertThat(manager.api().zones()).isInstanceOf(Route53ZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("accesskey", "A");
    map.put("secretkey", "S");
    manager = create("route53", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(Route53ZoneApi.class);
  }

  @Test
  public void testProviderWiresRoute53ZoneApiWithSessionCredentials() {
    DNSApiManager manager = create(PROVIDER, credentials("accesskey", "secretkey", "sessionToken"));
    assertThat(manager.api().zones()).isInstanceOf(Route53ZoneApi.class);
    manager = create("route53", credentials("accesskey", "secretkey", "sessionToken"));
    assertThat(manager.api().zones()).isInstanceOf(Route53ZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "no credentials supplied. route53 requires one of the following forms: when type is accessKey: accessKey,secretKey; session: accessKey,secretKey,sessionToken");
    // manually passing anonymous in case this test is executed from EC2
    // where IAM profiles are present.
    create(PROVIDER, anonymous()).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new Route53Provider()), new Route53Provider.Module(),
                credentials("accesskey", "secretkey"))
        .get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(Route53ZoneApi.class);
  }
}
