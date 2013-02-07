package denominator;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
 */
public interface DNSApi {
    /**
     * controls DNS zones, such as {@code netflix.com.}, availing information
     * about name servers and extended configuration.
     */
    ZoneApi getZoneApi();
}
