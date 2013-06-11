package denominator.dynect;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.dynect.v3.domain.GeoRegionGroup;
import org.jclouds.dynect.v3.domain.GeoService;
import org.jclouds.dynect.v3.domain.Node;
import org.jclouds.dynect.v3.domain.RecordSet;
import org.jclouds.dynect.v3.domain.RecordSet.Value;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;

final class GeoServiceToResourceRecordSets implements Function<GeoService, Iterable<ResourceRecordSet<?>>> {

    private final Function<List<String>, Multimap<String, String>> countryIndexer;
    private final Predicate<GeoRegionGroup> geoGroupFilter;
    private final Predicate<RecordSet> rsetFilter;

    /**
     * @param countryIndexer
     *            {@link Geo#regions()} is indexed, but
     *            {@link GeoRegionGroup#getCountries()} is not. This function
     *            will index the countries so that they match the denominator
     *            model.
     */
    @Inject
    GeoServiceToResourceRecordSets(
            @denominator.config.profile.Geo Function<List<String>, Multimap<String, String>> countryIndexer) {
        this(countryIndexer, Predicates.<GeoRegionGroup> alwaysTrue(), Predicates.<RecordSet> alwaysTrue());
    }

    private GeoServiceToResourceRecordSets(Function<List<String>, Multimap<String, String>> countryIndexer,
            Predicate<GeoRegionGroup> geoGroupFilter, Predicate<RecordSet> rsetFilter) {
        this.countryIndexer = checkNotNull(countryIndexer, "countryIndexer");
        this.geoGroupFilter = checkNotNull(geoGroupFilter, "geoGroupFilter");
        this.rsetFilter = checkNotNull(rsetFilter, "rsetFilter");
    }

    @Override
    public Iterable<ResourceRecordSet<?>> apply(GeoService input) {
        return FluentIterable.from(input.getGroups())
                             .filter(geoGroupFilter)
                             .transformAndConcat(toRRSetBuildersForEachGroup)
                             .transformAndConcat(new BuildForEachNode(input.getNodes())).toList();
    }

    private Function<GeoRegionGroup, Iterable<Builder<?>>> toRRSetBuildersForEachGroup = new Function<GeoRegionGroup, Iterable<Builder<?>>>() {
        @Override
        public Iterable<Builder<?>> apply(GeoRegionGroup group) {
            Geo geo = Geo.create(group.getName(), countryIndexer.apply(group.getCountries()));
            return FluentIterable.from(group.getRecordSets())
                                 .filter(rsetFilter)
                                 .transform(new ToResourceRecordSetBuilder(geo));
        }
    };

    GeoServiceToResourceRecordSets group(final String group) {
        return new GeoServiceToResourceRecordSets(countryIndexer, new Predicate<GeoRegionGroup>() {
            @Override
            public boolean apply(GeoRegionGroup input) {
                return group.equals(input.getName());
            }
        }, rsetFilter);
    }

    GeoServiceToResourceRecordSets type(final String type) {
        return new GeoServiceToResourceRecordSets(countryIndexer, geoGroupFilter, new Predicate<RecordSet>() {
            @Override
            public boolean apply(RecordSet input) {
                return type.equals(input.getType());
            }
        });
    }

    private static class BuildForEachNode implements
            Function<ResourceRecordSet.Builder<?>, Iterable<ResourceRecordSet<?>>> {
        private final Iterable<Node> nodes;

        private BuildForEachNode(Iterable<Node> nodes) {
            this.nodes = nodes;
        }

        @Override
        public Iterable<ResourceRecordSet<?>> apply(ResourceRecordSet.Builder<?> in) {
            ImmutableList.Builder<ResourceRecordSet<?>> rrsets = ImmutableList.builder();
            for (Node node : nodes) {
                rrsets.add(in.name(node.getFQDN()).build());
            }
            return rrsets.build();
        }
    }

    /**
     * the dynect {@code RecordSet} doesn't include
     * {@link ResourceRecordSet#name()}. This collects all details except the
     * name. The result of this function would be applied to all relevant
     * {@link GeoService#getNodes() nodes}.
     * 
     */
    private static class ToResourceRecordSetBuilder implements Function<RecordSet, ResourceRecordSet.Builder<?>> {
        private final Geo geo;

        public ToResourceRecordSetBuilder(Geo geo) {
            this.geo = geo;
        }

        @Override
        public ResourceRecordSet.Builder<?> apply(RecordSet in) {
            ResourceRecordSet.Builder<Map<String, Object>> builder = ResourceRecordSet.builder();
            builder.type(in.getType());
            builder.ttl(in.getTTL());
            for (Value val : in) {
                builder.add(val.getRData());
            }
            builder.addProfile(geo);
            return builder;
        }
    }
}
