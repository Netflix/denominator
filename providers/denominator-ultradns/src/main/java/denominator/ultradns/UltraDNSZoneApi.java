package denominator.ultradns;

import static denominator.model.Zones.toZone;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.IdAndName;
import org.jclouds.ultradns.ws.domain.Zone;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

public final class UltraDNSZoneApi implements denominator.ZoneApi {
    private final UltraDNSWSApi api;
    private final Supplier<IdAndName> account;

    @Inject
    UltraDNSZoneApi(UltraDNSWSApi api, Supplier<IdAndName> account) {
        this.api = api;
        this.account = account;
    }

    /**
     * in UltraDNS, zones are scoped to an account.
     */
    @Override
    public Iterator<denominator.model.Zone> iterator() {
        return api.getZoneApi().listByAccount(account.get().getId()).transform(ZoneName.INSTANCE).transform(toZone()).iterator();
    }

    private static enum ZoneName implements Function<Zone, String> {
        INSTANCE;
        public String apply(Zone input) {
            return input.getName();
        }
    }
}