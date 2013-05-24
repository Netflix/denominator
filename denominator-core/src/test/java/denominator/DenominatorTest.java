package denominator;

import org.testng.annotations.Test;

import denominator.mock.MockProvider;

public class DenominatorTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "provider foo not in set of configured providers: \\[mock\\]")
    public void testNiceMessageWhenProviderNotFound() {
        Denominator.create("foo");
    }
    
    static class NoModuleProvider extends BasicProvider {

    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*NoModuleProvider should have a static inner class named Module")
    public void testIllegalArgumentWhenMissingModule() {
        Denominator.create(new NoModuleProvider());
    }

    static class WrongCtorModuleProvider extends BasicProvider {

        @dagger.Module(injects = DNSApiManager.class, includes = MockProvider.Module.class)
        static class Module {
            Module(String name) {

            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "ensure .*Module has a no-args constructor")
    public void testIllegalArgumentWhenCtorHasArgs() {
        Denominator.create(new WrongCtorModuleProvider());
    }
}
