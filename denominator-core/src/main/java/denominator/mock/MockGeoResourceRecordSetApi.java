package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.toProfile;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.base.Optional;
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
    @Deprecated
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group) {
        return getByNameTypeAndQualifier(name, type, group);
    }

    @Override
    @Deprecated
    public void applyRegionsToNameTypeAndGroup(Multimap<String, String> regions, String name, String type, String group) {
        applyRegionsToNameTypeAndQualifier(regions, name, type, group);
    }

    @Override
    public void applyRegionsToNameTypeAndQualifier(Multimap<String, String> regions, String name, String type, String qualifier) {
        checkNotNull(regions, "regions");
        Optional<ResourceRecordSet<?>> existing = getByNameTypeAndQualifier(name, type, qualifier);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        Geo geo = toProfile(Geo.class).apply(rrset);
        if (geo.regions().equals(regions))
            return;
        ResourceRecordSet<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>> builder()
                                                                       .name(rrset.name())
                                                                       .type(rrset.type())
                                                                       .qualifier(qualifier)
                                                                       .ttl(rrset.ttl().orNull())
                                                                       // TODO: remove qualifier here in 2.0
                                                                       .addProfile(Geo.create(qualifier, regions))
                                                                       .addAll(rrset).build();
        replace(rrs);
    }

    @Override
    @Deprecated
    public void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group) {
        applyTTLToNameTypeAndGroup(ttl, name, type, group);
    }

    @Override
    public void applyTTLToNameTypeAndQualifier(int ttl, String name, String type, String qualifier) {
        checkNotNull(ttl, "ttl");
        Optional<ResourceRecordSet<?>> existing = getByNameTypeAndQualifier(name, type, qualifier);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        if (rrset.ttl().isPresent() && rrset.ttl().get().equals(ttl))
            return;
        ResourceRecordSet<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>> builder()
                                                                       .name(rrset.name())
                                                                       .type(rrset.type())
                                                                       .qualifier(qualifier)
                                                                       .ttl(ttl)
                                                                       .profile(rrset.profiles())
                                                                       .addAll(rrset).build();
        replace(rrs);
    }

    protected void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        checkArgument(rrset.qualifier().isPresent(), "no qualifier on: %s", rrset);
        checkArgument(profileContainsType(Geo.class).apply(rrset), "no geo profile found: %s", rrset);
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset
                .qualifier().get());
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
        records.put(zone, rrset);
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
