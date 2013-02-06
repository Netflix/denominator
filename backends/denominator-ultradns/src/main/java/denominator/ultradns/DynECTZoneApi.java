package denominator.ultradns;

import java.util.List;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.Zone;

import com.google.common.base.Function;

final class UltraDNSZoneApi implements denominator.ZoneApi {
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

    @Override
    public List<String> list() {
        return api.getZoneApi().listByAccount(api.getCurrentAccount().getId()).transform(ZoneName.INSTANCE).toList();
    }
}