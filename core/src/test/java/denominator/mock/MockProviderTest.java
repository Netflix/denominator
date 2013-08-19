package denominator.mock;

import static denominator.Denominator.create;
import static denominator.Providers.list;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import denominator.Provider;

public class MockProviderTest {
    private static final Provider PROVIDER = new MockProvider();

    @Test
    public void testMockMetadata() {
        assertEquals(PROVIDER.name(), "mock");
        assertEquals(PROVIDER.supportsDuplicateZoneNames(), false);
        assertEquals(PROVIDER.credentialTypeToParameterNames(), ImmutableMap.of());
    }

    @Test
    public void testMockRegistered() {
        Set<Provider> allProviders = ImmutableSet.copyOf(list());
        assertTrue(allProviders.contains(PROVIDER));
    }

    @Test
    public void testProviderWiresMockZoneApi() {
        assertEquals(create(PROVIDER).api().zones().getClass(), MockZoneApi.class);
        assertEquals(create("mock").api().zones().getClass(), MockZoneApi.class);
    }
}
