package denominator;

import javax.inject.Inject;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.profile.WeightedResourceRecordSetApi;

/**
 * allows you to manipulate resources such as DNS Zones and Records.
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
   * controls DNS zones, such as {@code netflix.com.}, availing information about name servers.
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
   * The argument to this is {@code zoneId}. It is possible that some zones do not have an id, and
   * in this case the name is used. The following form will ensure you get a reference regardless.
   *
   * <pre>
   * api.basicRecordSetsInZone(zone.idOrName());
   * </pre>
   *
   * <br> <br> <b>Beta</b><br>
   *
   * This is marked beta until the denominator 2.0 model is finalized. If this interface is
   * unaffected following that, we'll remove the Beta status.
   *
   * @param idOrName id of the zone, or its name if absent.
   * @see Zone#idOrName
   */
  public ResourceRecordSetApi basicRecordSetsInZone(String idOrName) {
    return rrsetApiFactory.create(idOrName);
  }

  /**
   * Controls all DNS records as a set. Operates against the zone with id {@code zoneId}. This is
   * supported by all {@link Provider providers}, but may only return basic records, if that's all
   * that is supported.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is {@code zoneId}. It is possible that some zones do not have an id, and
   * in this case the name is used. The following form will ensure you get a reference regardless.
   *
   * <pre>
   * api.recordSetsInZone(zone.idOrName());
   * </pre>
   *
   * <br> <br> <b>Beta</b><br>
   *
   * This is marked beta until the denominator 2.0 model is finalized. If this interface is
   * unaffected following that, we'll remove the Beta status.
   *
   * @param idOrName id of the zone, or its name if absent.
   * @see Zone#idOrName
   */
  public AllProfileResourceRecordSetApi recordSetsInZone(String idOrName) {
    return allRRSetApiFactory.create(idOrName);
  }

  /**
   * Controls DNS records which take into consideration the territory of the caller. These are
   * otherwise known as Directional records.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is {@code zoneId}. It is possible that some zones do not have an id, and
   * in this case the name is used. The following form will ensure you get a reference regardless.
   *
   * <pre>
   * api.geoRecordSetsInZone(zone.idOrName());
   * </pre>
   *
   * @param idOrName id of the zone, or its name if absent.
   * @return null if this feature isn't supported on the provider.
   * @see Zone#idOrName
   */
  public GeoResourceRecordSetApi geoRecordSetsInZone(String idOrName) {
    return geoApiFactory.create(idOrName);
  }

  /**
   * Controls DNS records which take into consideration the load of traffic from the caller. These
   * are otherwise known as weighted records.
   *
   * <br> <br> <b>Usage</b><br>
   *
   * The argument to this is {@code zoneId}. It is possible that some zones do not have an id, and
   * in this case the name is used. The following form will ensure you get a reference regardless.
   *
   * <pre>
   * api.weightedRecordSetsInZone(zone.idOrName());
   * </pre>
   *
   * @param idOrName id of the zone, or its name if absent.
   * @return null if this feature isn't supported on the provider.
   * @see Zone#idOrName
   */
  public WeightedResourceRecordSetApi weightedRecordSetsInZone(String idOrName) {
    return weightedApiFactory.create(idOrName);
  }
}
