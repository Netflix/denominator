package denominator.dynect;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;

public final class DynECTZoneApi implements denominator.ZoneApi {
    private final DynECTApi api;

    @Inject
    DynECTZoneApi(DynECTApi api) {
        this.api = api;
    }

    @Override
    public Iterator<String> list() {
        return api.getZoneApi().list().iterator();
    }
}