package denominator.mock;

import javax.inject.Inject;

import denominator.DNSApi;

public class MockNetworkServices implements DNSApi {
    private final MockZoneApi zoneApi;

    @Inject
    MockNetworkServices(MockZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public MockZoneApi getZoneApi() {
        return zoneApi;
    }
}
