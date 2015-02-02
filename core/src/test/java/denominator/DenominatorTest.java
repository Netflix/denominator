package denominator;

import org.testng.annotations.Test;

import dagger.ObjectGraph;
import denominator.mock.MockProvider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

public class DenominatorTest {

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "provider foo not in set of configured providers: \\[mock\\]")
  public void niceMessageWhenProviderNotFound() {
    Denominator.create("foo");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*NoModuleProvider should have a static inner class named Module")
  public void illegalArgumentWhenMissingModule() {
    Denominator.create(new NoModuleProvider());
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "ensure .*Module has a no-args constructor")
  public void illegalArgumentWhenCtorHasArgs() {
    Denominator.create(new WrongCtorModuleProvider());
  }

  @Test
  public void providerBindsProperly() {
    Provider provider = Denominator.create(new FooProvider()).provider();
    assertEquals(provider, new FooProvider());
  }

  @Test
  @Deprecated
  public void providerReturnsSameInstance() {
    FooProvider provider = new FooProvider();
    DNSApiManager
        mgr =
        ObjectGraph.create(Denominator.provider(provider), new FooProvider.Module()).get(
            DNSApiManager.class);
    assertSame(mgr.provider(), provider);
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
    assertEquals(provider.name(), "bar");
    assertEquals(provider.url(), "http://bar");
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
