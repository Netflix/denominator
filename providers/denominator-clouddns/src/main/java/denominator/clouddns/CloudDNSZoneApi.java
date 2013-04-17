package denominator.clouddns;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.domain.Domain;

import com.google.common.base.Function;

public final class CloudDNSZoneApi implements denominator.ZoneApi {
    private final CloudDNSApi api;

    @Inject
    CloudDNSZoneApi(CloudDNSApi api) {
        this.api = api;
    }

    public Iterator<String> list() {
        return api.getDomainApi().list().concat().transform(DomainName.INSTANCE).iterator();
    }

    private static enum DomainName implements Function<Domain, String> {
        INSTANCE;
        public String apply(Domain input) {
            return input.getName();
        }
    }
}
