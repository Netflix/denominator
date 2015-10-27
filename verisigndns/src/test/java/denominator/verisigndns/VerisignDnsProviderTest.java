package denominator.verisigndns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Providers.list;
import static denominator.Providers.provide;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import dagger.ObjectGraph;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Provider;

public class VerisignDnsProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static final Provider PROVIDER = new VerisignDnsProvider();

  @Test
  public void testVerisignDnsMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("verisigndns");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames()).containsEntry("password",
        Arrays.asList("username", "password"));
  }

  @Test
  public void testVerisignDnsRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresVerisignDnsZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(VerisignDnsZoneApi.class);
    manager = create("verisigndns", credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(VerisignDnsZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("username", "U");
    map.put("password", "P");
    manager = create("verisigndns", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(VerisignDnsZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. " + PROVIDER.name() + " requires username,password");

    create(PROVIDER).api().zones().iterator().hasNext();
  }

  @Test
  public void testTwoPartCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. " + PROVIDER.name() + " requires username,password");

    create(PROVIDER, credentials("customer", "username", "password")).api().zones().iterator().hasNext();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager =
        ObjectGraph.create(provide(new VerisignDnsProvider()), new VerisignDnsProvider.Module(),
            credentials("username", "password")).get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(VerisignDnsZoneApi.class);
  }
}
