package denominator;

import static com.google.common.base.Preconditions.checkArgument;

import javax.inject.Inject;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import denominator.model.Zone;
import denominator.model.Zones;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
 */
public class DNSApi {
    private final ZoneApi zones;
    private final ResourceRecordSetApi.Factory rrsetApiFactory;
    private final AllProfileResourceRecordSetApi.Factory allRRSetApiFactory;
    private final GeoResourceRecordSetApi.Factory geoApiFactory;

    @Inject
    DNSApi(ZoneApi zones, ResourceRecordSetApi.Factory rrsetApiFactory,
            AllProfileResourceRecordSetApi.Factory allRRSetApiFactory, GeoResourceRecordSetApi.Factory geoApiFactory) {
        this.zones = zones;
        this.rrsetApiFactory = rrsetApiFactory;
        this.allRRSetApiFactory = allRRSetApiFactory;
        this.geoApiFactory = geoApiFactory;
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #zones}
     */
    @Deprecated
    public ZoneApi getZoneApi() {
        return zones();
    }

    /**
     * controls DNS zones, such as {@code netflix.com.}, availing information
     * about name servers.
     */
    public ZoneApi zones() {
        return zones;
    }

    /**
     * controls DNS records as a set. Operates against the first zone named
     * zone {@code idOrName}.
     * 
     * @param idOrName
     *            name of the zone containing the record sets.
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #basicRecordSetsInZone}
     */
    @Deprecated
    public ResourceRecordSetApi getResourceRecordSetApiForZone(String zoneName) {
        return basicRecordSetsInZone(idOrName(zoneName));
    }

    /**
     * Controls basic DNS records as a set. Operates against the zone with id
     * {@code zoneId}. This api will not affect or return advanced records such
     * as {@link Geo}, and is supported by all {@link Provider providers}.
     * 
     * <h4>Usage</h4>
     * 
     * The argument to this is {@code zoneId}. It is possible that some zones do
     * not have an id, and in this case the name is used. The following form
     * will ensure you get a reference regardless.
     * 
     * <pre>
     * api.basicRecordSetsInZone(zone.idOrName());
     * </pre>
     * 
     * <h4>Beta</h4>
     * 
     * This is marked beta until the denominator 2.0 model is finalized. If this
     * interface is unaffected following that, we'll remove the Beta status.
     * 
     * @param idOrName
     *            id of the zone, or its name if absent.
     * @see Zone#idOrName
     */
    @Beta
    public ResourceRecordSetApi basicRecordSetsInZone(String idOrName) {
        return rrsetApiFactory.create(idOrName);
    }

    /**
     * allows you to list all resource record sets regardless of their profile.
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #recordSetsInZone}
     */
    @Deprecated
    public AllProfileResourceRecordSetApi getAllProfileResourceRecordSetApiForZone(String zoneName) {
        return recordSetsInZone(idOrName(zoneName));
    }

    /**
     * Controls all DNS records as a set. Operates against the zone with id
     * {@code zoneId}. This is supported by all {@link Provider providers}, but
     * may only return basic records, if that's all that is supported.
     * 
     * <h4>Usage</h4>
     * 
     * The argument to this is {@code zoneId}. It is possible that some zones do
     * not have an id, and in this case the name is used. The following form
     * will ensure you get a reference regardless.
     * 
     * <pre>
     * api.recordSetsInZone(zone.idOrName());
     * </pre>
     * 
     * <h4>Beta</h4>
     * 
     * This is marked beta until the denominator 2.0 model is finalized. If this
     * interface is unaffected following that, we'll remove the Beta status.
     * 
     * @param idOrName
     *            id of the zone, or its name if absent.
     * @see Zone#idOrName
     */
    public AllProfileResourceRecordSetApi recordSetsInZone(String idOrName) {
        return allRRSetApiFactory.create(idOrName);
    }

    /**
     * controls DNS records which take into consideration the territory of the
     * caller. These are otherwise known as Directional records.
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #geoRecordSetsInZone}
     */
    @Deprecated
    public Optional<GeoResourceRecordSetApi> getGeoResourceRecordSetApiForZone(String zoneName) {
        return geoRecordSetsInZone(idOrName(zoneName));
    }

    /**
     * Controls DNS records which take into consideration the territory of the
     * caller. These are otherwise known as Directional records.
     * 
     * <h4>Usage</h4>
     * 
     * The argument to this is {@code zoneId}. It is possible that some zones do
     * not have an id, and in this case the name is used. The following form
     * will ensure you get a reference regardless.
     * 
     * <pre>
     * api.geoRecordSetsInZone(zone.idOrName());
     * </pre>
     * 
     * <h4>Beta</h4>
     * 
     * This is marked beta until the denominator 2.0 model is finalized. If this
     * interface is unaffected following that, we'll remove the Beta status.
     * 
     * @param idOrName
     *            id of the zone, or its name if absent.
     * @see Zone#idOrName
     */
    public Optional<GeoResourceRecordSetApi> geoRecordSetsInZone(String idOrName) {
        return geoApiFactory.create(idOrName);
    }

    String idOrName(String zoneName) {
        Optional<Zone> zone = Iterators.tryFind(zones.iterator(), Zones.nameEqualTo(zoneName));
        checkArgument(zone.isPresent(), "zone %s not found", zoneName);
        return zone.get().idOrName();
    }
}
