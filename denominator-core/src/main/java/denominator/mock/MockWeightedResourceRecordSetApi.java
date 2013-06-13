package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.profileContainsType;

import java.util.Set;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.Provider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;
import denominator.profile.WeightedResourceRecordSetApi;

public final class MockWeightedResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements
        WeightedResourceRecordSetApi {
    private static final Predicate<ResourceRecordSet<?>> IS_WEIGHTED = profileContainsType(Weighted.class);

    private final Set<String> supportedTypes;
    private final SortedSet<Integer> supportedWeights;

    MockWeightedResourceRecordSetApi(Provider provider, Multimap<Zone, ResourceRecordSet<?>> records, Zone zone,
            SortedSet<Integer> supportedWeights) {
        super(provider, records, zone);
        this.supportedTypes = provider.profileToRecordTypes().get("weighted");
        this.supportedWeights = supportedWeights;
    }

    @Override
    public SortedSet<Integer> supportedWeights() {
        return supportedWeights;
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for weighted: %s", rrset.type(),
                supportedTypes);
        put(IS_WEIGHTED, rrset);
    }

    public static final class Factory implements WeightedResourceRecordSetApi.Factory {

        private final Multimap<Zone, ResourceRecordSet<?>> records;
        private Provider provider;
        private final SortedSet<Integer> supportedWeights;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<Zone, ResourceRecordSet> records, Provider provider,
                @Named("weighted") SortedSet<Integer> supportedWeights) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), IS_WEIGHTED));
            this.provider = provider;
            this.supportedWeights = supportedWeights;
        }

        @Override
        public Optional<WeightedResourceRecordSetApi> create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return Optional.<WeightedResourceRecordSetApi> of(new MockWeightedResourceRecordSetApi(provider, records,
                    zone, supportedWeights));
        }
    }
}
