package denominator.dynect;

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

public class DynECTProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static final Provider PROVIDER = new DynECTProvider();

  @Test
  public void testDynECTMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("dynect");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames())
        .containsEntry("password", Arrays.asList("customer", "username", "password"));
  }

  @Test
  public void testDynECTRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresDynECTZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("customer", "username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(DynECTZoneApi.class);
    manager = create("dynect", credentials("customer", "username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(DynECTZoneApi.class);
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("customer", "C");
    map.put("username", "U");
    map.put("password", "P");
    manager = create("dynect", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(DynECTZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. dynect requires customer,username,password");

    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testThreePartCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(
        "incorrect credentials supplied. dynect requires customer,username,password");

    create(PROVIDER, credentials("username", "password")).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new DynECTProvider()), new DynECTProvider.Module(),
                credentials("customer", "username", "password"))
        .get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(DynECTZoneApi.class);
  }
}
