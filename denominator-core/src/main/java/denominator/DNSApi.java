package denominator;

import javax.inject.Inject;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
 */
public class DNSApi {
    private final ZoneApi zoneApi;

    @Inject
    DNSApi(ZoneApi zoneApi) {
        this.zoneApi = zoneApi;
    }

    /**
     * controls DNS zones, such as {@code netflix.com.}, availing information
     * about name servers and extended configuration.
     */
    public ZoneApi getZoneApi() {
        return zoneApi;
    }
}
