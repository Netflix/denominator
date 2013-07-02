package denominator.mock;

import static denominator.common.Preconditions.checkArgument;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.profile.Geo.asGeo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

public final class MockGeoResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements
        GeoResourceRecordSetApi {
    private static final Filter<ResourceRecordSet<?>> IS_GEO = profileContainsType("geo");

    private final Collection<String> supportedTypes;
    private final Map<String, Collection<String>> supportedRegions;

    MockGeoResourceRecordSetApi(Provider provider, SortedSet<ResourceRecordSet<?>> records,
            Map<String, Collection<String>> supportedRegions) {
        super(provider, records, IS_GEO);
        this.supportedTypes = provider.profileToRecordTypes().get("geo");
        this.supportedRegions = supportedRegions;
    }

    @Override
    public Map<String, Collection<String>> supportedRegions() {
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
                Map<String, Collection<String>> without = new LinkedHashMap<String, Collection<String>>();
                for (Entry<String, Collection<String>> entry : currentGeo.regions().entrySet()) {
                    if (newGeo.regions().containsKey(entry.getKey())) {
                        List<String> territories = new ArrayList<String>(entry.getValue().size());
                        for (String territory : entry.getValue()) {
                            if (!newGeo.regions().get(entry.getKey()).contains(territory)) {
                                territories.add(territory);
                            }
                        }
                        without.put(entry.getKey(), territories);
                    } else {
                        without.put(entry.getKey(), entry.getValue());
                    }
                }
                records.remove(toTest);
                records.add(ResourceRecordSet.<Map<String, Object>> builder() //
                        .name(toTest.name())//
                        .type(toTest.type())//
                        .qualifier(toTest.qualifier())//
                        .ttl(toTest.ttl())//
                        .addProfile(Geo.create(without))//
                        .addAll(toTest.rdata()).build());
            }
        }
    }

    public static final class Factory implements GeoResourceRecordSetApi.Factory {

        private final Map<Zone, SortedSet<ResourceRecordSet<?>>> records;
        private Provider provider;
        private final Map<String, Collection<String>> supportedRegions;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Map<Zone, SortedSet<ResourceRecordSet>> records, Provider provider,
                @Named("geo") Map<String, Collection<String>> supportedRegions) {
            this.records = Map.class.cast(records);
            this.provider = provider;
            this.supportedRegions = supportedRegions;
        }

        @Override
        public GeoResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return new MockGeoResourceRecordSetApi(provider, records.get(zone), supportedRegions);
        }
    }
}
