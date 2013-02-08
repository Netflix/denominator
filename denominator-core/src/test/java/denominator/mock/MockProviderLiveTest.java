package denominator.mock;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import denominator.BaseProviderLiveTest;
import denominator.Denominator;

@Test
public class MockProviderLiveTest extends BaseProviderLiveTest {
    @BeforeClass
    private void setUp() {
        manager = Denominator.create(new MockProvider());
    }
}
