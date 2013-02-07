package denominator.dynect;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;

import com.google.common.collect.FluentIterable;

public final class DynECTZoneApi implements denominator.ZoneApi {
    private final DynECTApi api;

    @Inject
    DynECTZoneApi(DynECTApi api) {
        this.api = api;
    }

    @Override
    public FluentIterable<String> list() {
        return api.getZoneApi().list();
    }
}