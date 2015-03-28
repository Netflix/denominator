package denominator.ultradns;

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

public class UltraDNSProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static final Provider PROVIDER = new UltraDNSProvider();

  @Test
  public void testUltraDNSMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("ultradns");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames())
        .containsEntry("password", Arrays.asList("username", "password"));
  }

  @Test
  public void testUltraDNSRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresUltraDNSZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSZoneApi.class);
    manager = create("ultradns", credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("username", "U");
    map.put("password", "P");
    manager = create("ultradns", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. ultradns requires username,password");

    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testTwoPartCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. ultradns requires username,password");

    create(PROVIDER, credentials("customer", "username", "password")).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new UltraDNSProvider()), new UltraDNSProvider.Module(),
                credentials("username", "password"))
        .get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSZoneApi.class);
  }
}
