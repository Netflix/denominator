package denominator.route53;

import javax.inject.Inject;

import denominator.DNSApi;

public class Route53Api implements DNSApi {
    private final Route53ZoneApi zoneApi;

    @Inject
    Route53Api(Route53ZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public Route53ZoneApi getZoneApi() {
        return zoneApi;
    }
}
