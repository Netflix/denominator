package denominator.route53;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.route53.Route53Api;
import org.jclouds.route53.domain.HostedZone;

import com.google.common.base.Function;

import denominator.model.Zone;

public final class Route53ZoneApi implements denominator.ZoneApi {
    private final Route53Api api;

    @Inject
    Route53ZoneApi(Route53Api api) {
        this.api = api;
    }

    @Override
    public Iterator<Zone> iterator() {
        return api.getHostedZoneApi().list().concat().transform(ToZone.INSTANCE).iterator();
    }

    private static enum ToZone implements Function<HostedZone, Zone> {
        INSTANCE;
        public Zone apply(HostedZone input) {
            return Zone.create(input.getName(), input.getId());
        }
    }
}