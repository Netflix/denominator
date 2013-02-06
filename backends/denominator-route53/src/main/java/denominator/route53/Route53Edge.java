package denominator.route53;

import javax.inject.Inject;

import denominator.Edge;

public class Route53Edge implements Edge {
    private final Route53ZoneApi zoneApi;

    @Inject
    Route53Edge(Route53ZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public Route53ZoneApi getZoneApi() {
        return zoneApi;
    }
}
