package denominator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import dagger.ObjectGraph;
import denominator.mock.MockProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class DenominatorTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void niceMessageWhenProviderNotFound() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("provider foo not in set of configured providers: [mock]");

    Denominator.create("foo");
  }

  @Test
  public void illegalArgumentWhenMissingModule() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("NoModuleProvider should have a static inner class named Module");

    Denominator.create(new NoModuleProvider());
  }

  @Test
  public void illegalArgumentWhenCtorHasArgs() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Module has a no-args constructor");

    Denominator.create(new WrongCtorModuleProvider());
  }

  @Test
  public void providerBindsProperly() {
    Provider provider = Denominator.create(new FooProvider()).provider();
    assertThat(provider).isEqualTo(new FooProvider());
  }

  @Test
  @Deprecated
  public void providerReturnsSameInstance() {
    FooProvider provider = new FooProvider();
    DNSApiManager
        mgr =
        ObjectGraph.create(Denominator.provider(provider), new FooProvider.Module()).get(
            DNSApiManager.class);
    assertThat(mgr.provider()).isSameAs(provider);
  }

  @Test
  public void anonymousProviderPermitted() {
    Provider provider = Denominator.create(new FooProvider() {
      @Override
      public String name() {
        return "bar";
      }

      @Override
      public String url() {
        return "http://bar";
      }
    }).provider();
    assertThat(provider.name()).isEqualTo("bar");
    assertThat(provider.url()).isEqualTo("http://bar");
  }

  static class NoModuleProvider extends BasicProvider {

  }

  static class WrongCtorModuleProvider extends BasicProvider {

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

      Module(String name) {
      }
    }
  }

  static class FooProvider extends BasicProvider {

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }
}
