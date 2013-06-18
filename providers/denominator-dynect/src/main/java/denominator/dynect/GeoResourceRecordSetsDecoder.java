package denominator.dynect;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;

class GeoResourceRecordSetsDecoder implements Function<JsonReader, Multimap<String, ResourceRecordSet<?>>> {

    private final Gson gson = new Gson();
    private final ToBuilders toBuilders;

    /**
     * @param countryIndexer
     *            {@link Geo#regions()} is indexed, but
     *            {@link GeoRegionGroup#getCountries()} is not. This function
     *            will index the countries so that they match the denominator
     *            model.
     */
    @Inject
    GeoResourceRecordSetsDecoder(@Named("geo") Function<List<String>, Multimap<String, String>> countryIndexer) {
        this.toBuilders = new ToBuilders(countryIndexer);
    }

    @SuppressWarnings("serial")
    private static final TypeToken<List<GeoService>> GEO_TOKEN = new TypeToken<List<GeoService>>() {
    };

    @Override
    public Multimap<String, ResourceRecordSet<?>> apply(JsonReader reader) {
        List<GeoService> geoServices = gson.fromJson(reader, GEO_TOKEN.getType());
        ImmutableMultimap.Builder<String, ResourceRecordSet<?>> rrsets = ImmutableMultimap.builder();
        for (GeoService geo : geoServices) {
            for (Builder<?> rrset : FluentIterable.from(geo.groups).transformAndConcat(toBuilders)) {
                for (Node node : geo.nodes) {
                    rrsets.put(node.zone, rrset.name(node.fqdn).build());
                }
            }
        }
        return rrsets.build();
    }

    private static class GeoService {
        List<Node> nodes = Lists.newArrayList();
        List<GeoRegionGroup> groups = Lists.newArrayList();
    }

    private class Node {
        String zone;
        String fqdn;
    }

    private static class GeoRegionGroup {
        String service_name;
        String name;
        // aaaa_weight
        Map<String, List<Integer>> weight = Maps.newLinkedHashMap();
        List<String> countries = Lists.newArrayList();
        // spf_rdata
        Map<String, List<JsonElement>> rdata = Maps.newLinkedHashMap();
        // dhcid_ttl
        Map<String, Integer> ttl = Maps.newLinkedHashMap();
    }

    private static class ToBuilders implements Function<GeoRegionGroup, List<ResourceRecordSet.Builder<?>>> {
        private Function<List<String>, Multimap<String, String>> countryIndexer;

        private ToBuilders(Function<List<String>, Multimap<String, String>> countryIndexer) {
            this.countryIndexer = countryIndexer;
        }

        @Override
        public ImmutableList<Builder<?>> apply(GeoRegionGroup creepyGeoRegionGroup) {
            ImmutableList.Builder<ResourceRecordSet.Builder<?>> rrsets = ImmutableList.builder();
            Geo geo = Geo.create(countryIndexer.apply(creepyGeoRegionGroup.countries));

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
                rrset.addProfile(geo);
                // weight is only present for a couple record types
                List<Integer> weights = creepyGeoRegionGroup.weight.get(type.toLowerCase() + "_weight");
                if (weights != null && !weights.isEmpty())
                    rrset.addProfile(Weighted.create(weights.get(0)));

                for (int i = 0; i < entry.getValue().size(); i++) {
                    rrset.add(ToRecord.toRData(entry.getValue().get(i).getAsJsonObject()));
                }
                rrsets.add(rrset);
            }
            return rrsets.build();
        }
    }
}
