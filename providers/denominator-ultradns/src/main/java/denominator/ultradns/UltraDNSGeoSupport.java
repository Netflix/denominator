package denominator.ultradns;

import java.util.SortedSet;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table.Cell;

import dagger.Module;
import dagger.Provides;

@Module(injects = UltraDNSGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSGeoSupport {

    @Provides
    @Singleton
    @Named("geo")
    Multimap<String, String> regions(UltraDNS api) {
        Builder<String, String> regions = ImmutableMultimap.<String, String> builder();
        for (Cell<String, Integer, SortedSet<String>> region : api.getRegionsByIdAndName().cellSet()) {
            regions.putAll(region.getRowKey(), region.getValue());
        }
        return regions.build();
    }

    @Provides
    @Singleton
    @Named("geo")
    CacheLoader<String, Multimap<String, String>> getDirectionalGroup(final UltraDNS api) {
        return new CacheLoader<String, Multimap<String, String>>() {

            @Override
            public Multimap<String, String> load(String key) throws Exception {
                return api.getDirectionalGroup(key).regionToTerritories;
            }

        };
    }
}
