package denominator.mock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.testng.annotations.Test;

import java.util.Set;

import denominator.Provider;

import static denominator.Denominator.create;
import static denominator.Providers.list;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MockProviderTest {

  private static final Provider PROVIDER = new MockProvider();

  @Test
  public void testMockMetadata() {
    assertEquals(PROVIDER.name(), "mock");
    assertEquals(PROVIDER.supportsDuplicateZoneNames(), false);
    assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMap.of());
  }

  @Test
  public void testMockRegistered() {
    Set<Provider> allProviders = ImmutableSet.copyOf(list());
    assertTrue(allProviders.contains(PROVIDER));
  }

  @Test
  public void testProviderWiresMockZoneApi() {
    assertEquals(create(PROVIDER).api().zones().getClass(), MockZoneApi.class);
    assertEquals(create("mock").api().zones().getClass(), MockZoneApi.class);
  }
}
