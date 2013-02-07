package denominator.dynect;

import javax.inject.Inject;

import denominator.DNSApi;

public class DynECTApi implements DNSApi {
    private final DynECTZoneApi zoneApi;

    @Inject
    DynECTApi(DynECTZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public DynECTZoneApi getZoneApi() {
        return zoneApi;
    }
}
