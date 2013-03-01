package denominator;

import javax.inject.Inject;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
 */
public class DNSApi {
    private final ZoneApi zoneApi;
    private final ResourceRecordSetApi.Factory rrsetApiFactory;

    @Inject
    DNSApi(ZoneApi zoneApi, ResourceRecordSetApi.Factory rrsetApiFactory) {
        this.zoneApi = zoneApi;
        this.rrsetApiFactory = rrsetApiFactory;
    }

    /**
     * controls DNS zones, such as {@code netflix.com.}, availing information
     * about name servers and extended configuration.
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
}
