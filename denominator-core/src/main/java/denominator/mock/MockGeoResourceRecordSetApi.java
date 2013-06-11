package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.toProfile;
import static denominator.model.profile.Geos.groupEqualTo;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

public final class MockGeoResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements GeoResourceRecordSetApi {

    private final Multimap<String, String> regions;
    private final Set<String> types;

    MockGeoResourceRecordSetApi(Multimap<Zone, ResourceRecordSet<?>> records, Multimap<String, String> regions,
            Set<String> types, Zone zone) {
        super(records, zone);
        this.regions = regions;
        this.types = types;
    }

    @Override
    @Deprecated
    public Set<String> getSupportedTypes() {
        return supportedTypes();
    }

    @Override
    public Set<String> supportedTypes() {
        return types;
    }

    @Override
    @Deprecated
    public Multimap<String, String> getSupportedRegions() {
        return supportedRegions();
    }

    @Override
    public Multimap<String, String> supportedRegions() {
        return regions;
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(type, "group");
        return from(records.get(zone))
                .firstMatch(and(nameAndTypeEqualTo(name, type), geoGroupEqualTo(group)));
    }

    @Override
    public void applyRegionsToNameTypeAndGroup(Multimap<String, String> regions, String name, String type, String group) {
        checkNotNull(regions, "regions");
        Optional<ResourceRecordSet<?>> existing = getByNameTypeAndGroup(name, type, group);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        Geo geo = toProfile(Geo.class).apply(rrset);
        if (geo.regions().equals(regions))
            return;
        ResourceRecordSet<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>> builder()
                                                                       .name(rrset.name())
                                                                       .type(rrset.type())
                                                                       .ttl(rrset.ttl().orNull())
                                                                       .addProfile(Geo.create(geo.group(), regions))
                                                                       .addAll(rrset).build();
        replace(rrs);
    }

    @Override
    public void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group) {
        checkNotNull(ttl, "ttl");
        Optional<ResourceRecordSet<?>> existing = getByNameTypeAndGroup(name, type, group);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        if (rrset.ttl().isPresent() && rrset.ttl().get().equals(ttl))
            return;
        ResourceRecordSet<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>> builder()
                                                                       .name(rrset.name())
                                                                       .type(rrset.type())
                                                                       .ttl(ttl)
                                                                       .profile(rrset.profiles())
                                                                       .addAll(rrset).build();
        replace(rrs);
    }

    private void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(profileContainsType(Geo.class).apply(rrset), "no geo profile found: %s", rrset);
        Geo geo = toProfile(Geo.class).apply(rrset);
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameTypeAndGroup(rrset.name(), rrset.type(), geo.group());
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
        records.put(zone, rrset);
    }

    private static Predicate<ResourceRecordSet<?>> geoGroupEqualTo(String group) {
        return compose(groupEqualTo(group), toProfile(Geo.class));
    }

    public static final class Factory implements GeoResourceRecordSetApi.Factory {

        private final Multimap<Zone, ResourceRecordSet<?>> records;
        private final Multimap<String, String> regions;
        private final Set<String> types;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<Zone, ResourceRecordSet> records,
                @denominator.config.profile.Geo Multimap<String, String> regions,
                @denominator.config.profile.Geo Set<String> types) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), profileContainsType(Geo.class)));
            this.regions = regions;
            this.types = types;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return Optional.<GeoResourceRecordSetApi> of(
                    new MockGeoResourceRecordSetApi(records, regions, types, zone));
        }
    }
}
