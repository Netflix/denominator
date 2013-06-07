package denominator.dynect;

import static com.google.common.collect.Iterators.transform;
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
    public Iterator<String> list() {
        return api.getZoneApi().list().iterator();
    }

    @Override
    public Iterator<Zone> iterator() {
        return transform(list(), toZone());
    }
}