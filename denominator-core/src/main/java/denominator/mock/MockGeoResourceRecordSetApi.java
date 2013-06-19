package denominator.mock;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.profile.Geo.asGeo;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.Provider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

public final class MockGeoResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements
        GeoResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");

    private final Set<String> supportedTypes;
    private final Multimap<String, String> supportedRegions;

    MockGeoResourceRecordSetApi(Provider provider, Multimap<Zone, ResourceRecordSet<?>> records, Zone zone,
            Multimap<String, String> supportedRegions) {
        super(provider, records, zone);
        this.supportedTypes = provider.profileToRecordTypes().get("geo");
        this.supportedRegions = supportedRegions;
    }

    @Override
    @Deprecated
    public Set<String> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    @Deprecated
    public Multimap<String, String> getSupportedRegions() {
        return supportedRegions();
    }

    @Override
    public Multimap<String, String> supportedRegions() {
        return supportedRegions;
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s", rrset.type(),
                supportedTypes);
        synchronized (records) {
            put(IS_GEO, rrset);
            Geo newGeo = asGeo(rrset);
            Iterator<ResourceRecordSet<?>> iterateByNameAndType = iterateByNameAndType(rrset.name(), rrset.type());
            while (iterateByNameAndType.hasNext()) {
                ResourceRecordSet<?> toTest = iterateByNameAndType.next();
                if (toTest.qualifier().equals(rrset.qualifier()))
                    continue;
                Geo currentGeo = asGeo(toTest);
                Multimap<String, String> without = filterValues(currentGeo.regions(),
                        not(in(newGeo.regions().values())));
                currentGeo = Geo.create(ImmutableMultimap.copyOf(without));
                records.remove(zone, toTest);
                records.put(zone, ResourceRecordSet.<Map<String, Object>> builder() //
                        .name(toTest.name())//
                        .type(toTest.type())//
                        .qualifier(toTest.qualifier().get())//
                        .ttl(toTest.ttl().orNull())//
                        .addProfile(currentGeo)//
                        .addAll(toTest.rdata()).build());
            }
        }
    }

    @Override
    @Deprecated
    public Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group) {
        return getByNameTypeAndQualifier(name, type, group);
    }

    @Override
    @Deprecated
    public void applyRegionsToNameTypeAndGroup(Multimap<String, String> regions, String name, String type, String group) {
        checkNotNull(regions, "regions");
        Optional<ResourceRecordSet<?>> existing = getByNameTypeAndQualifier(name, type, group);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        Geo geo = asGeo(rrset);
        if (geo.regions().equals(regions))
            return;
        ResourceRecordSet<Map<String, Object>> rrs = ResourceRecordSet.<Map<String, Object>> builder()
                .name(rrset.name()).type(rrset.type()).qualifier(group).ttl(rrset.ttl().orNull())
                .addProfile(Geo.create(regions)).addAll(rrset).build();
        put(IS_GEO, rrs);
    }

    @Override
    @Deprecated
    public void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group) {
        checkNotNull(ttl, "ttl");
        Optional<ResourceRecordSet<?>> existing = getByNameTypeAndQualifier(name, type, group);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        if (rrset.ttl().isPresent() && rrset.ttl().get().equals(ttl))
            return;
        ResourceRecordSet<Map<String, Object>> rrs = ResourceRecordSet.<Map<String, Object>> builder()
                .name(rrset.name()).type(rrset.type()).qualifier(group).ttl(ttl).profile(rrset.profiles())
                .addAll(rrset).build();
        put(IS_GEO, rrs);
    }

    public static final class Factory implements GeoResourceRecordSetApi.Factory {

        private final Multimap<Zone, ResourceRecordSet<?>> records;
        private Provider provider;
        private final Multimap<String, String> supportedRegions;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<Zone, ResourceRecordSet> records, Provider provider,
                @Named("geo") Multimap<String, String> supportedRegions) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), IS_GEO));
            this.provider = provider;
            this.supportedRegions = supportedRegions;
        }

        @Override
        public Optional<GeoResourceRecordSetApi> create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return Optional.<GeoResourceRecordSetApi> of(new MockGeoResourceRecordSetApi(provider, records, zone,
                    supportedRegions));
        }
    }
}
