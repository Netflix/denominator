package denominator.designate;

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

public class DesignateProviderTest {

  private static final Provider PROVIDER = new DesignateProvider();

  @Test
  public void testMockMetadata() {
    assertEquals(PROVIDER.name(), "designate");
    assertEquals(PROVIDER.supportsDuplicateZoneNames(), true);
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
    assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
    manager = create("designate", credentials("tenantId", "username", "password"));
    assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("tenantId", "T");
    map.put("username", "U");
    map.put("password", "P");
    manager = create("designate", credentials(MapCredentials.from(map)));
    assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. designate requires tenantId,username,password")
  public void testCredentialsRequired() {
    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new DesignateProvider()), new DesignateProvider.Module(),
                credentials("tenantId", "username", "password"))
        .get(DNSApiManager.class);
    assertEquals(manager.api().zones().getClass(), DesignateZoneApi.class);
  }
}
