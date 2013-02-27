package denominator;

import org.testng.annotations.Test;

public class DenominatorTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "provider foo not in set of configured providers: \\[mock\\]")
    public void testNiceMessageWhenProviderNotFound() {
        Denominator.create("foo");
    }
}
