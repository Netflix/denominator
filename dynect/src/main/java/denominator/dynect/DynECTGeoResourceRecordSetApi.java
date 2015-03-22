package denominator.dynect;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import denominator.common.Filter;
import denominator.dynect.DynECT.GeoService;
import denominator.dynect.DynECT.GeoService.GeoRegionGroup;
import denominator.dynect.DynECT.GeoService.Node;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;
import denominator.profile.GeoResourceRecordSetApi;

import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.and;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;

public final class DynECTGeoResourceRecordSetApi implements GeoResourceRecordSetApi {

  private static final Filter<ResourceRecordSet<?>> IS_GEO = new Filter<ResourceRecordSet<?>>() {
    @Override
    public boolean apply(ResourceRecordSet<?> in) {
      return in != null && in.geo() != null;
    }
  };

  private final Map<String, Collection<String>> regions;
  private final ToBuilders toBuilders;
  private final DynECT api;
  private final String zoneFQDN;

  DynECTGeoResourceRecordSetApi(Map<String, Collection<String>> regions, DynECT api,
                                String zoneFQDN) {
    this.regions = regions;
    this.toBuilders = new ToBuilders(regions);
    this.api = api;
    this.zoneFQDN = zoneFQDN;
  }

  @Override
  public Map<String, Collection<String>> supportedRegions() {
    return regions;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    Map<String, Collection<ResourceRecordSet<?>>>
        rrsets =
        new LinkedHashMap<String, Collection<ResourceRecordSet<?>>>();
    for (GeoService geo : api.geoServices().data) {
      for (GeoRegionGroup geoGroup : geo.groups) {
        for (Builder<?> rrset : toBuilders.apply(geoGroup)) {
          for (Node node : geo.nodes) {
            if (!rrsets.containsKey(node.zone)) {
              rrsets.put(node.zone, new ArrayList<ResourceRecordSet<?>>());
            }
            rrsets.get(node.zone).add(rrset.name(node.fqdn).build());
          }
        }
      }
    }
    Collection<ResourceRecordSet<?>> val = rrsets.get(zoneFQDN);
    return val != null ? val.iterator() : Collections.<ResourceRecordSet<?>>emptyList().iterator();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return filter(iterator(), and(nameEqualTo(name), IS_GEO));
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    return filter(iterator(), and(nameAndTypeEqualTo(name, type), IS_GEO));
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    return nextOrNull(
        filter(iterator(), and(nameTypeAndQualifierEqualTo(name, type, qualifier), IS_GEO)));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    throw new UnsupportedOperationException();
  }

  static final class Factory implements GeoResourceRecordSetApi.Factory {

    private final Map<String, Collection<String>> regions;
    private final Lazy<Boolean> hasAllGeoPermissions;
    private final DynECT api;

    @Inject
    Factory(@Named("geo") Map<String, Collection<String>> regions,
            @Named("hasAllGeoPermissions") Lazy<Boolean> hasAllGeoPermissions, DynECT api) {
      this.regions = regions;
      this.hasAllGeoPermissions = hasAllGeoPermissions;
      this.api = api;
    }

    @Override
    public GeoResourceRecordSetApi create(String name) {
      checkNotNull(name, "name was null");
      if (!hasAllGeoPermissions.get()) {
        return null;
      }
      return new DynECTGeoResourceRecordSetApi(regions, api, name);
    }
  }

  private static class ToBuilders {

    private final Map<String, Collection<String>> regions;
    private final Map<String, String> countryToRegions;

    private ToBuilders(Map<String, Collection<String>> regions) {
      this.regions = regions;
      Map<String, String> countryToRegions = new HashMap<String, String>(regions.values().size());
      for (Entry<String, Collection<String>> entry : regions.entrySet()) {
        for (String country : entry.getValue()) {
          countryToRegions.put(country, entry.getKey());
        }
      }
      this.countryToRegions = countryToRegions;
    }

    public List<Builder<?>> apply(GeoRegionGroup creepyGeoRegionGroup) {
      List<ResourceRecordSet.Builder<?>> rrsets = new ArrayList<ResourceRecordSet.Builder<?>>();
      Geo geo = Geo.create(indexCountries(creepyGeoRegionGroup.countries));

      for (Entry<String, List<JsonElement>> entry : creepyGeoRegionGroup.rdata.entrySet()) {
        if (entry.getValue().isEmpty()) {
          continue;
        }
        // ex. spf_rdata -> SPF
        String type = entry.getKey().substring(0, entry.getKey().indexOf('_')).toUpperCase();
        // ex. dhcid_ttl
        int ttl = creepyGeoRegionGroup.ttl.get(type.toLowerCase() + "_ttl");
        ResourceRecordSet.Builder<Map<String, Object>> rrset = ResourceRecordSet.builder();
        rrset.type(type);
        rrset.qualifier(creepyGeoRegionGroup.name != null ? creepyGeoRegionGroup.name
                                                          : creepyGeoRegionGroup.service_name);
        rrset.ttl(ttl);
        rrset.geo(geo);
        // weight is only present for a couple record types
        List<Integer> weights = creepyGeoRegionGroup.weight.get(type.toLowerCase() + "_weight");
        if (weights != null && !weights.isEmpty()) {
          rrset.weighted(Weighted.create(weights.get(0)));
        }

        for (int i = 0; i < entry.getValue().size(); i++) {
          rrset.add(ToRecord.toRData(type, entry.getValue().get(i).getAsJsonObject()));
        }
        rrsets.add(rrset);
      }
      return rrsets;
    }

    private Map<String, Collection<String>> indexCountries(List<String> countries) {
      Map<String, Collection<String>> indexed = new LinkedHashMap<String, Collection<String>>();
      for (String country : countries) {
        // special case the "all countries" condition
        if (regions.containsKey(country)) {
          indexed.put(country, Arrays.asList(country));
        } else if (countryToRegions.containsKey(country)) {
          String region = countryToRegions.get(country);
          if (!indexed.containsKey(region)) {
            indexed.put(region, new ArrayList<String>());
          }
          indexed.get(region).add(country);
        } else {
          // TODO log not found
        }
      }
      return indexed;
    }
  }
}
