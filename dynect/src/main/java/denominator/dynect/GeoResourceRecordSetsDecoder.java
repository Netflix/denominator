package denominator.dynect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;

class GeoResourceRecordSetsDecoder implements DynECTDecoder.Parser<Map<String, Collection<ResourceRecordSet<?>>>> {

    private final Gson gson;
    private final ToBuilders toBuilders;

    @Inject
    GeoResourceRecordSetsDecoder(Gson gson, @Named("geo") final Map<String, Collection<String>> regions) {
        this.gson = gson;
        this.toBuilders = new ToBuilders(regions);
    }

    private static final TypeToken<List<GeoService>> GEO_TOKEN = new TypeToken<List<GeoService>>() {
    };

    public Map<String, Collection<ResourceRecordSet<?>>> apply(JsonReader reader) throws IOException {
        List<GeoService> geoServices;
        try {
            geoServices = gson.fromJson(reader, GEO_TOKEN.getType());
        } catch (JsonIOException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw IOException.class.cast(e.getCause());
            }
            throw e;
        }
        Map<String, Collection<ResourceRecordSet<?>>> rrsets = new LinkedHashMap<String, Collection<ResourceRecordSet<?>>>();
        for (GeoService geo : geoServices) {
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
        return rrsets;
    }

    private static class GeoService {
        List<Node> nodes = new ArrayList<Node>();
        List<GeoRegionGroup> groups = new ArrayList<GeoRegionGroup>();
    }

    private class Node {
        String zone;
        String fqdn;
    }

    private static class GeoRegionGroup {
        String service_name;
        String name;
        // aaaa_weight
        Map<String, List<Integer>> weight = new LinkedHashMap<String, List<Integer>>();
        List<String> countries = new ArrayList<String>();
        // spf_rdata
        Map<String, List<JsonElement>> rdata = new LinkedHashMap<String, List<JsonElement>>();
        // dhcid_ttl
        Map<String, Integer> ttl = new LinkedHashMap<String, Integer>();
    }

    private static class ToBuilders {
        private final Map<String, Collection<String>> regions;
        private final Map<String, String> countryToRegions;

        private ToBuilders(Map<String, Collection<String>> regions) {
            this.regions = regions;
            Map<String, String> countryToRegions = new HashMap<String, String>(regions.values().size());
            for (Entry<String, Collection<String>> entry : regions.entrySet()) {
                for (String country : entry.getValue())
                    countryToRegions.put(country, entry.getKey());
            }
            this.countryToRegions = countryToRegions;
        }

        public List<Builder<?>> apply(GeoRegionGroup creepyGeoRegionGroup) {
            List<ResourceRecordSet.Builder<?>> rrsets = new ArrayList<ResourceRecordSet.Builder<?>>();
            Geo geo = Geo.create(indexCountries(creepyGeoRegionGroup.countries));

            for (Entry<String, List<JsonElement>> entry : creepyGeoRegionGroup.rdata.entrySet()) {
                if (entry.getValue().isEmpty())
                    continue;
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
                if (weights != null && !weights.isEmpty())
                    rrset.weighted(Weighted.create(weights.get(0)));

                for (int i = 0; i < entry.getValue().size(); i++) {
                    rrset.add(ToRecord.toRData(entry.getValue().get(i).getAsJsonObject()));
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
