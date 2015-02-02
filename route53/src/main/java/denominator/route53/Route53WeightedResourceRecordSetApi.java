package denominator.route53;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Named;

import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.profile.WeightedResourceRecordSetApi;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Util.filter;

final class Route53WeightedResourceRecordSetApi implements WeightedResourceRecordSetApi {

  private static final Filter<ResourceRecordSet<?>>
      IS_WEIGHTED =
      new Filter<ResourceRecordSet<?>>() {
        @Override
        public boolean apply(ResourceRecordSet<?> in) {
          return in != null && in.weighted() != null;
        }
      };

  private final Collection<String> supportedTypes;
  private final SortedSet<Integer> supportedWeights;

  private final Route53AllProfileResourceRecordSetApi allApi;

  Route53WeightedResourceRecordSetApi(Provider provider, SortedSet<Integer> supportedWeights,
                                      Route53AllProfileResourceRecordSetApi allProfileResourceRecordSetApi) {
    this.supportedTypes = provider.profileToRecordTypes().get("weighted");
    this.supportedWeights = supportedWeights;
    this.allApi = allProfileResourceRecordSetApi;
  }

  @Override
  public SortedSet<Integer> supportedWeights() {
    return supportedWeights;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return filter(allApi.iterator(), IS_WEIGHTED);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return filter(allApi.iterateByName(name), IS_WEIGHTED);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    return filter(allApi.iterateByNameAndType(name, type), IS_WEIGHTED);
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    return allApi.getByNameTypeAndQualifier(name, type, qualifier);
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for weighted: %s",
                  rrset.type(),
                  supportedTypes);
    allApi.put(rrset);
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    allApi.deleteByNameTypeAndQualifier(name, type, qualifier);
  }

  static final class Factory implements WeightedResourceRecordSetApi.Factory {

    private final Provider provider;
    private final SortedSet<Integer> supportedWeights;
    private final Route53AllProfileResourceRecordSetApi.Factory allApi;

    @Inject
    Factory(Provider provider, @Named("weighted") SortedSet<Integer> supportedWeights,
            Route53AllProfileResourceRecordSetApi.Factory allApi) {
      this.provider = provider;
      this.supportedWeights = supportedWeights;
      this.allApi = allApi;
    }

    @Override
    public WeightedResourceRecordSetApi create(String id) {
      return new Route53WeightedResourceRecordSetApi(provider, supportedWeights, allApi.create(id));
    }
  }
}
