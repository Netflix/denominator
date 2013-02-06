package denominator.stub;

import javax.inject.Inject;

import denominator.Edge;

public class StubEdge implements Edge {
    private final StubZoneApi zoneApi;

    @Inject
    StubEdge(StubZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public StubZoneApi getZoneApi() {
        return zoneApi;
    }
}
