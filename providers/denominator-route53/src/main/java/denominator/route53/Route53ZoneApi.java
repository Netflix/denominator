package denominator.route53;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.route53.Route53Api;
import org.jclouds.route53.domain.HostedZone;

import com.google.common.base.Function;

public final class Route53ZoneApi implements denominator.ZoneApi {
    private final Route53Api api;

    @Inject
    Route53ZoneApi(Route53Api api) {
        this.api = api;
    }

    @Override
    public Iterator<String> list() {
        return api.getHostedZoneApi().list().concat().transform(ZoneName.INSTANCE).iterator();
    }

    private static enum ZoneName implements Function<HostedZone, String> {
        INSTANCE;
        public String apply(HostedZone input) {
            return input.getName();
        }
    }
}