package denominator.ultradns;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.Zone;

import com.google.common.base.Function;

public final class UltraDNSZoneApi implements denominator.ZoneApi {
    private final UltraDNSWSApi api;

    @Inject
    UltraDNSZoneApi(UltraDNSWSApi api) {
        this.api = api;
    }

    private static enum ZoneName implements Function<Zone, String> {
        INSTANCE;
        public String apply(Zone input) {
            return input.getName();
        }
    }

    /**
     * in UltraDNS, zones are scoped to an account.
     */
    @Override
    public Iterator<String> list() {
        return api.getZoneApi().listByAccount(api.getCurrentAccount().getId()).transform(ZoneName.INSTANCE).iterator();
    }
}