package denominator.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

final class MockGeoResourceRecordSetApi extends MockAllProfileResourceRecordSetApi
    implements GeoResourceRecordSetApi {

  private static final Filter<ResourceRecordSet<?>> IS_GEO = new Filter<ResourceRecordSet<?>>() {
    @Override
    public boolean apply(ResourceRecordSet<?> in) {
      return in != null && in.geo() != null;
    }
  };

  private final Map<String, Collection<String>> supportedRegions;

  MockGeoResourceRecordSetApi(Map<String, Collection<ResourceRecordSet<?>>> data, String zoneName,
                              Map<String, Collection<String>> supportedRegions) {
    super(data, zoneName, IS_GEO);
    this.supportedRegions = supportedRegions;
  }

  @Override
  public Map<String, Collection<String>> supportedRegions() {
    return supportedRegions;
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    Collection<ResourceRecordSet<?>> records = records();
    synchronized (records) {
      put(IS_GEO, rrset);
      Geo newGeo = rrset.geo();
      Iterator<ResourceRecordSet<?>> nameAndType = iterateByNameAndType(rrset.name(), rrset.type());
      while (nameAndType.hasNext()) {
        ResourceRecordSet<?> toTest = nameAndType.next();
        if (toTest.qualifier().equals(rrset.qualifier())) {
          continue;
        }
        Geo currentGeo = toTest.geo();
        Map<String, Collection<String>> without = new LinkedHashMap<String, Collection<String>>();
        for (Entry<String, Collection<String>> entry : currentGeo.regions().entrySet()) {
          if (newGeo.regions().containsKey(entry.getKey())) {
            List<String> territories = new ArrayList<String>(entry.getValue().size());
            for (String territory : entry.getValue()) {
              if (!newGeo.regions().get(entry.getKey()).contains(territory)) {
                territories.add(territory);
              }
            }
            without.put(entry.getKey(), territories);
          } else {
            without.put(entry.getKey(), entry.getValue());
          }
        }
        records.remove(toTest);
        records.add(ResourceRecordSet.builder()
                        .name(toTest.name())
                        .type(toTest.type())
                        .qualifier(toTest.qualifier())
                        .ttl(toTest.ttl())
                        .geo(Geo.create(without))
                        .weighted(toTest.weighted())
                        .addAll(toTest.records()).build());
      }
    }
  }
}
