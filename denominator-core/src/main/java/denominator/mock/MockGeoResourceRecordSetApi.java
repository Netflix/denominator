package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.toProfile;
import static denominator.model.profile.Geos.nameEqualTo;

import java.util.Set;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

public final class MockGeoResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements GeoResourceRecordSetApi {

    private final Multimap<String, String> regions;
    private final Set<String> types;

    MockGeoResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> records, Multimap<String, String> regions,
            Set<String> types, String zoneName) {
        super(records, zoneName);
        this.regions = regions;
        this.types = types;
    }

    @Override
    public Set<String> getSupportedTypes() {
        return types;
    }

    @Override
    public Multimap<String, String> getSupportedRegions() {
        return regions;
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(type, "group");
        return from(records.get(zoneName))
                .firstMatch(and(nameAndTypeEqualTo(name, type), geoGroupNameEqualTo(group)));
    }

    private static Predicate<ResourceRecordSet<?>> geoGroupNameEqualTo(String group) {
        return compose(nameEqualTo(group), toProfile(Geo.class));
    }

    public static final class Factory implements GeoResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> records;
        private final Multimap<String, String> regions;
        private final Set<String> types;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> records,
                @denominator.config.profile.Geo Multimap<String, String> regions,
                @denominator.config.profile.Geo Set<String> types) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), profileContainsType(Geo.class)));
            this.regions = regions;
            this.types = types;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String zoneName) {
            checkArgument(records.keySet().contains(zoneName), "zone %s not found", zoneName);
            return Optional.<GeoResourceRecordSetApi> of(
                    new MockGeoResourceRecordSetApi(records, regions, types, zoneName));
        }
    }

}
