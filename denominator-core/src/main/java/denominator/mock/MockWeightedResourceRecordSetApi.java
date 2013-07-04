package denominator.mock;

import static denominator.common.Preconditions.checkArgument;
import static denominator.model.ResourceRecordSets.profileContainsType;

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.profile.WeightedResourceRecordSetApi;

public final class MockWeightedResourceRecordSetApi extends MockAllProfileResourceRecordSetApi implements
        WeightedResourceRecordSetApi {
    private static final Filter<ResourceRecordSet<?>> IS_WEIGHTED = profileContainsType("weighted");

    private final Collection<String> supportedTypes;
    private final SortedSet<Integer> supportedWeights;

    MockWeightedResourceRecordSetApi(Provider provider, SortedSet<ResourceRecordSet<?>> records,
            SortedSet<Integer> supportedWeights) {
        super(provider, records, IS_WEIGHTED);
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

        private final Map<Zone, SortedSet<ResourceRecordSet<?>>> records;
        private Provider provider;
        private final SortedSet<Integer> supportedWeights;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Map<Zone, SortedSet<ResourceRecordSet>> records, Provider provider,
                @Named("weighted") SortedSet<Integer> supportedWeights) {
            this.records = Map.class.cast(records);
            this.provider = provider;
            this.supportedWeights = supportedWeights;
        }

        @Override
        public WeightedResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return new MockWeightedResourceRecordSetApi(provider, records.get(zone), supportedWeights);
        }
    }
}
