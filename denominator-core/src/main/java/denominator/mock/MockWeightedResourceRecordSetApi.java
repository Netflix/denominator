package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;

import java.util.Set;
import java.util.SortedSet;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;
import denominator.profile.WeightedResourceRecordSetApi;

public final class MockWeightedResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements
        WeightedResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_WEIGHTED = profileContainsType(Weighted.class);

    private final Set<String> supportedTypes;
    private final SortedSet<Integer> supportedWeights;

    MockWeightedResourceRecordSetApi(Multimap<Zone, ResourceRecordSet<?>> records, Zone zone,
            Set<String> supportedTypes, SortedSet<Integer> supportedWeights) {
        super(records, zone);
        this.supportedTypes = supportedTypes;
        this.supportedWeights = supportedWeights;
    }

    @Override
    public Set<String> supportedTypes() {
        return supportedTypes;
    }

    @Override
    public SortedSet<Integer> supportedWeights() {
        return supportedWeights;
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        put(IS_WEIGHTED, rrset);
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameTypeAndQualifier(name, type, qualifier);
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
    }

    public static final class Factory implements WeightedResourceRecordSetApi.Factory {

        private final Multimap<Zone, ResourceRecordSet<?>> records;
        private final Set<String> supportedTypes;
        private final SortedSet<Integer> supportedWeights;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<Zone, ResourceRecordSet> records,
                @denominator.config.profile.Weighted Set<String> supportedTypes,
                @denominator.config.profile.Weighted SortedSet<Integer> supportedWeights) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), IS_WEIGHTED));
            this.supportedTypes = supportedTypes;
            this.supportedWeights = supportedWeights;
        }

        @Override
        public Optional<WeightedResourceRecordSetApi> create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return Optional.<WeightedResourceRecordSetApi> of(new MockWeightedResourceRecordSetApi(records,
                    zone, supportedTypes, supportedWeights));
        }
    }
}
