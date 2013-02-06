package denominator.dynect;

import java.util.List;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;

final class DynECTZoneApi implements denominator.ZoneApi {
    private final DynECTApi api;

    @Inject
    DynECTZoneApi(DynECTApi api) {
        this.api = api;
    }

    @Override
    public List<String> list() {
        return api.getZoneApi().list().toList();
    }
}