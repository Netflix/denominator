package denominator.ultradns;

import javax.inject.Inject;

import denominator.DNSApi;

public class UltraDNSApi implements DNSApi {
    private final UltraDNSZoneApi zoneApi;

    @Inject
    UltraDNSApi(UltraDNSZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public UltraDNSZoneApi getZoneApi() {
        return zoneApi;
    }
}
