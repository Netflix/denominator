package denominator.clouddns;

import static com.google.common.collect.Iterators.transform;
import static denominator.model.Zones.toName;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;

import com.google.common.base.Function;

import denominator.model.Zone;

public final class CloudDNSZoneApi implements denominator.ZoneApi {
    private final CloudDNSApi api;

    @Inject
    CloudDNSZoneApi(CloudDNSApi api) {
        this.api = api;
    }

    @Override
    public Iterator<String> list() {
        return transform(iterator(), toName());
    }

    @Override
    public Iterator<Zone> iterator() {
        return api.getDomainApi().list().concat().transform(ToZone.INSTANCE).iterator();
    }

    private static enum ToZone implements Function<Domain, Zone> {
        INSTANCE;
        public Zone apply(Domain input) {
            return Zone.create(input.getName(), String.valueOf(input.getId()));
        }
    }
}
