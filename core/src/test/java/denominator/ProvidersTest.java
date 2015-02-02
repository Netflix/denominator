package denominator;

import org.testng.annotations.Test;

import dagger.ObjectGraph;
import denominator.mock.MockProvider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class ProvidersTest {

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "provider foo not in set of configured providers: \\[mock\\]")
  public void niceMessageWhenProviderNotFound() {
    Providers.getByName("foo");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*NoModuleProvider should have a static inner class named Module")
  public void illegalArgumentWhenMissingModule() {
    Providers.instantiateModule(new NoModuleProvider());
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "ensure .*Module has a no-args constructor")
  public void illegalArgumentWhenModuleCtorHasArgs() {
    Providers.instantiateModule(new WrongCtorModuleProvider());
  }

  @Test
  public void provideReturnsSameInstance() {
    FooProvider provider = new FooProvider();
    DNSApiManager
        mgr =
        ObjectGraph.create(Providers.provide(provider), new FooProvider.Module()).get(
            DNSApiManager.class);
    assertSame(mgr.provider(), provider);
  }

  @Test
  public void withUrlOverrides() {
    Provider provider = Providers.withUrl(new URLProvider(), "http://bar");
    assertEquals(provider.url(), "http://bar");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "class .*FooProvider does not have a String parameter constructor")
  public void withUrlWhenProviderMissingStringCtor() {
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
    assertEquals(mgr.provider().name(), "bar");
    assertEquals(mgr.provider().url(), "http://bar");
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
