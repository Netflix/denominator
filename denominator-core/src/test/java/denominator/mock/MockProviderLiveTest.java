package denominator.mock;

import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;
import denominator.Denominator;

@Test
public class MockProviderLiveTest extends BaseProviderLiveTest {
    public MockProviderLiveTest() {
        manager = Denominator.create(new MockProvider());
        mutableZone = "denominator.io.";
    }
}
