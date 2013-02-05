package denominator.stub;

import org.testng.annotations.Test;

import denominator.Denominator;

public class DenominatorTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "backend foo not in set of configured backends: [stub]")
    public void testNiceMessageWhenBackendNotFound() {
        Denominator.connectToBackend("foo");
    }
}
