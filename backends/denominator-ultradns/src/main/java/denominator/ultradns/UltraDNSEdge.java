package denominator.ultradns;

import javax.inject.Inject;

import denominator.Edge;

public class UltraDNSEdge implements Edge {
    private final UltraDNSZoneApi zoneApi;

    @Inject
    UltraDNSEdge(UltraDNSZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public UltraDNSZoneApi getZoneApi() {
        return zoneApi;
    }
}
