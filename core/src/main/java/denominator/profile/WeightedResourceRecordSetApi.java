package denominator.profile;

import java.util.SortedSet;

import denominator.QualifiedResourceRecordSetApi;
import denominator.model.profile.Weighted;

/**
 * list operations are filtered to only include those which are weighted record sets.
 *
 * @since 1.3
 */
public interface WeightedResourceRecordSetApi extends QualifiedResourceRecordSetApi {

  /**
   * the set of {@link Weighted#weight() weights} that are valid for this provider. If present,
   * {@code 0} implies always serve this record set.
   */
  SortedSet<Integer> supportedWeights();

  static interface Factory extends QualifiedResourceRecordSetApi.Factory {

    /**
     * @return null if this feature isn't supported on the provider.
     */
    @Override
    WeightedResourceRecordSetApi create(String id);
  }
}
