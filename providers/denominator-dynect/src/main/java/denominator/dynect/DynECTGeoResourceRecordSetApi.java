package denominator.dynect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.filter;

import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import org.jclouds.dynect.v3.domain.GeoService;
import org.jclouds.dynect.v3.domain.Node;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

import denominator.dynect.DynECTProvider.ReadOnlyApi;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

public final class DynECTGeoResourceRecordSetApi implements GeoResourceRecordSetApi {

    private final Set<String> types;
    private final Multimap<String, String> regions;
    private final ReadOnlyApi api;
    private final GeoServiceToResourceRecordSets geoToRRSets;
    private final String zoneFQDN;

    DynECTGeoResourceRecordSetApi(Set<String> types, Multimap<String, String> regions, ReadOnlyApi api,
            GeoServiceToResourceRecordSets geoToRRSets, String zoneFQDN) {
        this.types = types;
        this.regions = regions;
        this.api = api;
        this.geoToRRSets = geoToRRSets;
        this.zoneFQDN = zoneFQDN;
    }

    @Override
    public Set<String> getSupportedTypes() {
        return types;
    }

    @Override
    public Multimap<String, String> getSupportedRegions() {
        return regions;
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return geoServices(inZone).transformAndConcat(geoToRRSets).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String fqdn) {
        checkNotNull(fqdn, "fqdn was null");
        return geoServices(withNode(fqdn)).transformAndConcat(geoToRRSets).iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String fqdn, String type) {
        checkNotNull(fqdn, "fqdn was null");
        checkNotNull(type, "type was null");
        return geoServices(withNode(fqdn)).transformAndConcat(geoToRRSets.type(type)).iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String fqdn, String type, String group) {
        checkNotNull(fqdn, "fqdn was null");
        checkNotNull(type, "type was null");
        checkNotNull(group, "group was null");
        return geoServices(withNode(fqdn)).transformAndConcat(geoToRRSets.type(type).group(group)).first();
    }

    /**
     * {@link GeoService} are an aggregation of nodes, which may not be in the
     * current zone. We need to filter out those not in the zone from our
     * results.
     */
    private FluentIterable<GeoService> geoServices(final Predicate<Node> nodeFilter) {
        return api.geos()
                  .filter(nodesMatching(nodeFilter))
                  .transform(retainNodes(nodeFilter));
    }

    @Override
    public void applyRegionsToNameTypeAndGroup(Multimap<String, String> regions, String name, String type, String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group) {
        throw new UnsupportedOperationException();
    }

    private Function<GeoService, GeoService> retainNodes(final Predicate<Node> nodeFilter) {
        return new Function<GeoService, GeoService>() {
            @Override
            public GeoService apply(GeoService input) {
                return input.toBuilder().nodes(filter(input.getNodes(), nodeFilter)).build();
            }
        };
    }

    private static Predicate<GeoService> nodesMatching(final Predicate<Node> nodeFilter) {
        return new Predicate<GeoService>() {
            @Override
            public boolean apply(GeoService input) {
                return any(input.getNodes(), nodeFilter);
            }
        };
    }

    private final Predicate<Node> inZone = new Predicate<Node>() {
        @Override
        public boolean apply(Node input) {
            return zoneFQDN.equals(input.getZone());
        }
    };

    private Predicate<Node> withNode(String fqdn) {
        return equalTo(Node.create(zoneFQDN, fqdn));
    }

    static final class Factory implements GeoResourceRecordSetApi.Factory {
        private final Set<String> types;
        private final Multimap<String, String> regions;
        private final ReadOnlyApi api;
        private final GeoServiceToResourceRecordSets geoToRRSets;

        @Inject
        Factory(@denominator.config.profile.Geo Set<String> types,
                @denominator.config.profile.Geo Multimap<String, String> regions, ReadOnlyApi api,
                GeoServiceToResourceRecordSets geoToRRSets) {
            this.types = types;
            this.regions = regions;
            this.api = api;
            this.geoToRRSets = geoToRRSets;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            checkNotNull(idOrName, "idOrName was null");
            return Optional.<GeoResourceRecordSetApi> of(
                    new DynECTGeoResourceRecordSetApi(types, regions, api, geoToRRSets, idOrName));
        }
    }
}
