package denominator.dynect;

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

public class DynECTProviderTest {

  private static final Provider PROVIDER = new DynECTProvider();

  @Test
  public void testMockMetadata() {
    assertEquals(PROVIDER.name(), "dynect");
    assertEquals(PROVIDER.supportsDuplicateZoneNames(), false);
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
    assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
    manager = create("dynect", credentials("customer", "username", "password"));
    assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("customer", "C");
    map.put("username", "U");
    map.put("password", "P");
    manager = create("dynect", credentials(MapCredentials.from(map)));
    assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "no credentials supplied. dynect requires customer,username,password")
  public void testCredentialsRequired() {
    create(PROVIDER).api().zones().iterator();
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "incorrect credentials supplied. dynect requires customer,username,password")
  public void testThreePartCredentialsRequired() {
    create(PROVIDER, credentials("username", "password")).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
        .create(provide(new DynECTProvider()), new DynECTProvider.Module(),
                credentials("customer", "username", "password"))
        .get(DNSApiManager.class);
    assertEquals(manager.api().zones().getClass(), DynECTZoneApi.class);
  }
}
