package denominator.route53;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static denominator.route53.ToDenominatorResourceRecordSet.isAlias;

import java.util.Iterator;

import javax.inject.Inject;

import org.jclouds.route53.Route53Api;
import org.jclouds.route53.domain.HostedZone;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

final class Route53ResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final org.jclouds.route53.features.ResourceRecordSetApi route53RRsetApi;

    Route53ResourceRecordSetApi(org.jclouds.route53.features.ResourceRecordSetApi route53RRsetApi) {
        this.route53RRsetApi = route53RRsetApi;
    }

    /**
     * lists and lazily transforms all record sets who are not aliases into denominator format.
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return route53RRsetApi.list().concat().filter(not(isAlias()))
                .transform(ToDenominatorResourceRecordSet.INSTANCE).iterator();
    }
    
    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Route53Api api;

        @Inject
        Factory(Route53Api api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(final String zoneName) {
            Optional<HostedZone> zone = api.getHostedZoneApi().list().concat().firstMatch(zoneNameEquals(zoneName));
            checkArgument(zone.isPresent(), "zone %s not found", zoneName);
            return new Route53ResourceRecordSetApi(api.getResourceRecordSetApiForHostedZone(zone.get().getId()));
        }
    }

    /**
     * Amazon Hosted Zones are addressed by id, not by name.
     */
    private static final Predicate<HostedZone> zoneNameEquals(final String zoneName) {
        checkNotNull(zoneName, "zoneName");
        return new Predicate<HostedZone>() {
            @Override
            public boolean apply(HostedZone input) {
                return input.getName().equals(zoneName);
            }
        };
    }
}