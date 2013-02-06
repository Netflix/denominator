package denominator.dynect;

import javax.inject.Inject;

import denominator.Edge;

public class DynECTEdge implements Edge {
    private final DynECTZoneApi zoneApi;

    @Inject
    DynECTEdge(DynECTZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    @Override
    public DynECTZoneApi getZoneApi() {
        return zoneApi;
    }
}
