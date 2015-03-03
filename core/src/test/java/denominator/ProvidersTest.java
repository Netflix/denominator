package denominator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import dagger.ObjectGraph;
import denominator.mock.MockProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class ProvidersTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void niceMessageWhenProviderNotFound() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("provider foo not in set of configured providers: [mock]");

    Providers.getByName("foo");
  }

  @Test
  public void illegalArgumentWhenMissingModule() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("NoModuleProvider should have a static inner class named Module");

    Providers.instantiateModule(new NoModuleProvider());
  }

  @Test
  public void illegalArgumentWhenModuleCtorHasArgs() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Module has a no-args constructor");

    Providers.instantiateModule(new WrongCtorModuleProvider());
  }

  @Test
  public void provideReturnsSameInstance() {
    FooProvider provider = new FooProvider();
    DNSApiManager
        mgr =
        ObjectGraph.create(Providers.provide(provider), new FooProvider.Module()).get(
            DNSApiManager.class);

    assertThat(mgr.provider()).isSameAs(provider);
  }

  @Test
  public void withUrlOverrides() {
    Provider provider = Providers.withUrl(new URLProvider(), "http://bar");
    assertThat(provider.url()).isEqualTo("http://bar");
  }

  @Test
  public void withUrlWhenProviderMissingStringCtor() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("FooProvider does not have a String parameter constructor");

    Providers.withUrl(new FooProvider(), "http://bar");
  }

  @Test
  public void anonymousProviderPermitted() {
    FooProvider provider = new FooProvider() {
      @Override
      public String name() {
        return "bar";
      }

      @Override
      public String url() {
        return "http://bar";
      }
    };
    DNSApiManager
        mgr =
        ObjectGraph.create(Providers.provide(provider), new FooProvider.Module()).get(
            DNSApiManager.class);
    assertThat(mgr.provider().name()).isEqualTo("bar");
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

  static class URLProvider extends BasicProvider {

    private final String url;

    public URLProvider() {
      this(null);
    }

    public URLProvider(String url) {
      this.url = url == null || url.isEmpty() ? "http://foo" : url;
    }

    @Override
    public String url() {
      return url;
    }

    @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class, complete = false)
    static class Module {

    }
  }
}
