package denominator;

import javax.inject.Inject;

import com.google.common.base.Optional;

import denominator.profile.GeoResourceRecordSetApi;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
 */
public class DNSApi {
    private final ZoneApi zoneApi;
    private final ResourceRecordSetApi.Factory rrsetApiFactory;
    private final AllProfileResourceRecordSetApi.Factory allRRSetApiFactory;
    private final GeoResourceRecordSetApi.Factory geoApiFactory;

    @Inject
    DNSApi(ZoneApi zoneApi, ResourceRecordSetApi.Factory rrsetApiFactory,
            AllProfileResourceRecordSetApi.Factory allRRSetApiFactory,
            GeoResourceRecordSetApi.Factory geoApiFactory) {
        this.zoneApi = zoneApi;
        this.rrsetApiFactory = rrsetApiFactory;
        this.allRRSetApiFactory = allRRSetApiFactory;
        this.geoApiFactory = geoApiFactory;
    }

    /**
     * controls DNS zones, such as {@code netflix.com.}, availing information
     * about name servers.
     */
    public ZoneApi getZoneApi() {
        return zoneApi;
    }

    /**
     * controls DNS records as a set,
     */
    public ResourceRecordSetApi getResourceRecordSetApiForZone(String zoneName) {
        return rrsetApiFactory.create(zoneName);
    }

    /**
     * allows you to list all resource record sets regardless of their profile.
     */
    public AllProfileResourceRecordSetApi getAllProfileResourceRecordSetApiForZone(String zoneName) {
        return allRRSetApiFactory.create(zoneName);
    }

    /**
     * controls DNS records which take into consideration the territory of the
     * caller. These are otherwise known as Directional records.
     */
    public Optional<GeoResourceRecordSetApi> getGeoResourceRecordSetApiForZone(String zoneName) {
        return geoApiFactory.create(zoneName);
    }
}
