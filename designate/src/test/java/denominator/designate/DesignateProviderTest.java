package denominator.designate;

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

public class DesignateProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();
  
  private static final Provider PROVIDER = new DesignateProvider();

  @Test
  public void testDesignateMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("designate");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames())
        .containsEntry("password", Arrays.asList("tenantId", "username", "password"));
  }

  @Test
  public void testDesignateRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresDesignateZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("tenantId", "username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(DesignateZoneApi.class);
    manager = create("designate", credentials("tenantId", "username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(DesignateZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("tenantId", "T");
    map.put("username", "U");
    map.put("password", "P");
    manager = create("designate", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(DesignateZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. designate requires tenantId,username,password");

    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new DesignateProvider()), new DesignateProvider.Module(),
                credentials("tenantId", "username", "password"))
        .get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(DesignateZoneApi.class);
  }
}
