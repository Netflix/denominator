package denominator.dynect;

import static denominator.model.Zones.toZone;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.dynect.v3.DynECTApi;

import denominator.model.Zone;

public final class DynECTZoneApi implements denominator.ZoneApi {
    private final DynECTApi api;

    @Inject
    DynECTZoneApi(DynECTApi api) {
        this.api = api;
    }

    @Override
    public Iterator<Zone> iterator() {
        return api.getZoneApi().list().transform(toZone()).iterator();
    }
}