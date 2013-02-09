package denominator.mock;

import javax.inject.Inject;

import denominator.DNSApi;

public class MockDNSApi implements DNSApi {
    private final MockZoneApi zoneApi;

    @Inject
    MockDNSApi(MockZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public MockZoneApi getZoneApi() {
        return zoneApi;
    }
}
