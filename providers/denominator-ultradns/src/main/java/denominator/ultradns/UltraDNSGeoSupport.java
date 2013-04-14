package denominator.ultradns;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV4;
import static org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType.IPV6;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Singleton;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.DirectionalPool.RecordType;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.profile.GeoResourceRecordSetApi;

@Module(entryPoints = DNSApiManager.class, complete = false)
public class UltraDNSGeoSupport {

    @Provides
    @Singleton
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(UltraDNSGeoResourceRecordSetApi.Factory in) {
        return in;
    }

    /**
     * directional pools in ultra have types {@code IPV4} and {@code IPV6} which
     * accept both CNAME and address types.
     */
    @Provides
    @Singleton
    @denominator.config.profile.Geo
    Set<String> provideSupportedGeoRecordTypes() {
        return ImmutableSet.<String> builder()
                .add("A", "AAAAA", "CNAME")
                .addAll(FluentIterable.from(EnumSet.allOf(RecordType.class))
                        .filter(not(in((ImmutableSet.of(IPV4, IPV6)))))
                        .transform(toStringFunction())).build();
    }

    @Provides
    @Singleton
    @denominator.config.profile.Geo
    Multimap<String, String> getRegions(UltraDNSWSApi api) {
        Builder<String, String> regions = ImmutableMultimap.<String, String> builder();
        for (Entry<IdAndName, Collection<String>> region : api.getRegionsByIdAndName().asMap().entrySet()) {
            regions.putAll(region.getKey().getName(), region.getValue());
        }
        return regions.build();
    }

    @Provides
    @Singleton
    @denominator.config.profile.Geo
    CacheLoader<String, Multimap<String, String>> getDirectionalGroup(final UltraDNSWSApi api,
            final Supplier<IdAndName> account) {
        return new CacheLoader<String, Multimap<String, String>>() {

            @Override
            public Multimap<String, String> load(String key) throws Exception {
                return ImmutableMultimap.copyOf(api.getDirectionalGroupApiForAccount(account.get().getId()).get(key));
            }

        };
    }
}
