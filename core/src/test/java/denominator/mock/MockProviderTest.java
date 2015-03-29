package denominator.mock;

import org.junit.Test;

import denominator.Provider;

import static denominator.Denominator.create;
import static denominator.Providers.list;
import static org.assertj.core.api.Assertions.assertThat;

public class MockProviderTest {

  private static final Provider PROVIDER = new MockProvider();

  @Test
  public void testMockMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("mock");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames()).isEmpty();
  }

  @Test
  public void testMockRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresMockZoneApi() {
    assertThat(create(PROVIDER).api().zones()).isInstanceOf(MockZoneApi.class);
    assertThat(create("mock").api().zones()).isInstanceOf(MockZoneApi.class);
  }
}
