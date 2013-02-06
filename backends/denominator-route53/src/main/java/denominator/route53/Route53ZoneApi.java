package denominator.route53;

import java.util.List;

import javax.inject.Inject;

import org.jclouds.route53.Route53Api;
import org.jclouds.route53.domain.Zone;

import com.google.common.base.Function;

final class Route53ZoneApi implements denominator.ZoneApi {
    private final Route53Api api;

    @Inject
    Route53ZoneApi(Route53Api api) {
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
        return api.getZoneApi().list().concat().transform(ZoneName.INSTANCE).toList();
    }
}