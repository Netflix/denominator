package denominator;

import javax.inject.Inject;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.profile.WeightedResourceRecordSetApi;

/**
 * Allows you to manipulate resources such as DNS Zones and Records.
 */
public class DNSApi {

  private final ZoneApi zones;
  private final ResourceRecordSetApi.Factory rrsetApiFactory;
  private final AllProfileResourceRecordSetApi.Factory allRRSetApiFactory;
  private final GeoResourceRecordSetApi.Factory geoApiFactory;
  private final WeightedResourceRecordSetApi.Factory weightedApiFactory;

  @Inject
  DNSApi(ZoneApi zones, ResourceRecordSetApi.Factory rrsetApiFactory,
         AllProfileResourceRecordSetApi.Factory allRRSetApiFactory,
         GeoResourceRecordSetApi.Factory geoApiFactory,
         WeightedResourceRecordSetApi.Factory weightedApiFactory) {
    this.zones = zones;
    this.rrsetApiFactory = rrsetApiFactory;
    this.allRRSetApiFactory = allRRSetApiFactory;
    this.geoApiFactory = geoApiFactory;
    this.weightedApiFactory = weightedApiFactory;
  }

  /**
   * Controls DNS zones, such as {@code netflix.com.}, availing information about name servers.
   */
  public ZoneApi zones() {
    return zones;
  }

  /**
   * Controls basic DNS records as a set. Operates against the zone with id {@code zoneId}. This api
   * will not affect or return advanced records such as {@link Geo}, and it is expected that no
   * record sets returned will contain a {@link ResourceRecordSet#qualifier()}. This api is
   * supported by all {@link Provider providers}.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is the {@link Zone#id() zone id}.  If unknown, lookup via {@link
   * denominator.ZoneApi#iterateByName(String)}.
   *
   * <pre>
   * api.basicRecordSetsInZone(zone.id());
   * </pre>
   *
   * @param id {@link Zone#id() id} of the zone.
   */
  public ResourceRecordSetApi basicRecordSetsInZone(String id) {
    return rrsetApiFactory.create(id);
  }

  /**
   * Controls all DNS records as a set. Operates against the zone with id {@code zoneId}. This is
   * supported by all {@link Provider providers}, but may only return basic records, if that's all
   * that is supported.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is the {@link Zone#id() zone id}.  If unknown, lookup via {@link
   * denominator.ZoneApi#iterateByName(String)}.
   *
   * <pre>
   * api.recordSetsInZone(zone.id());
   * </pre>
   *
   * @param id {@link Zone#id() id} of the zone.
   */
  public AllProfileResourceRecordSetApi recordSetsInZone(String id) {
    return allRRSetApiFactory.create(id);
  }

  /**
   * Controls DNS records which take into consideration the territory of the caller. These are
   * otherwise known as Directional records.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is the {@link Zone#id() zone id}.  If unknown, lookup via {@link
   * denominator.ZoneApi#iterateByName(String)}.
   *
   * <pre>
   * api.geoRecordSetsInZone(zone.id());
   * </pre>
   *
   * @param id {@link Zone#id() id} of the zone.
   * @return null if this feature isn't supported on the provider.
   */
  public GeoResourceRecordSetApi geoRecordSetsInZone(String id) {
    return geoApiFactory.create(id);
  }

  /**
   * Controls DNS records which take into consideration the load of traffic from the caller. These
   * are otherwise known as weighted records.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is the {@link Zone#id() zone id}.  If unknown, lookup via {@link
   * denominator.ZoneApi#iterateByName(String)}.
   *
   * <pre>
   * api.weightedRecordSetsInZone(zone.id());
   * </pre>
   *
   * @param id {@link Zone#id() id} of the zone.
   * @return null if this feature isn't supported on the provider.
   */
  public WeightedResourceRecordSetApi weightedRecordSetsInZone(String id) {
    return weightedApiFactory.create(id);
  }
}
