package denominator.ultradns;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Singleton;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.profile.GeoResourceRecordSetApi;

@Module(injects = DNSApiManager.class, complete = false)
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
        return ImmutableSet.of("A", "AAAA", "CNAME", "HINFO", "MX", "PTR", "RP", "SRV", "TXT", "NAPTR");
    }

    @Provides
    @Singleton
    @denominator.config.profile.Geo
    Multimap<String, String> regions(UltraDNSWSApi api) {
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
