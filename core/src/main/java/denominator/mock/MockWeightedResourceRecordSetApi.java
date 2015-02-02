package denominator.mock;

import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.profile.WeightedResourceRecordSetApi;

import static denominator.common.Preconditions.checkArgument;

public final class MockWeightedResourceRecordSetApi extends MockAllProfileResourceRecordSetApi
    implements
    WeightedResourceRecordSetApi {

  private static final Filter<ResourceRecordSet<?>>
      IS_WEIGHTED =
      new Filter<ResourceRecordSet<?>>() {
        @Override
        public boolean apply(ResourceRecordSet<?> in) {
          return in != null && in.weighted() != null;
        }
      };

  private final SortedSet<Integer> supportedWeights;

  MockWeightedResourceRecordSetApi(Provider provider, SortedSet<ResourceRecordSet<?>> records,
                                   SortedSet<Integer> supportedWeights) {
    super(provider, records, IS_WEIGHTED);
    this.supportedWeights = supportedWeights;
  }

  @Override
  public SortedSet<Integer> supportedWeights() {
    return supportedWeights;
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    put(IS_WEIGHTED, rrset);
  }

  public static final class Factory implements WeightedResourceRecordSetApi.Factory {

    private final Map<Zone, SortedSet<ResourceRecordSet<?>>> records;
    private final SortedSet<Integer> supportedWeights;
    private Provider provider;

    // unbound wildcards are not currently injectable in dagger
    @SuppressWarnings({"rawtypes", "unchecked"})
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
