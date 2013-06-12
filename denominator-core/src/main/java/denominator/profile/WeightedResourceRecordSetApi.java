package denominator.profile;

import java.util.SortedSet;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import denominator.QualifiedResourceRecordSetApi;
import denominator.model.profile.Weighted;

/**
 * list operations are filtered to only include those which are weighted record
 * sets.
 * 
 * @since 1.3
 */
@Beta
public interface WeightedResourceRecordSetApi extends QualifiedResourceRecordSetApi {

    /**
     * the set of {@link Weighted#weight() weights} that are valid for this
     * provider. If present, {@code 0} implies always serve this record set.
     */
    SortedSet<Integer> supportedWeights();

    static interface Factory extends QualifiedResourceRecordSetApi.Factory {
        @Override
        Optional<WeightedResourceRecordSetApi> create(String idOrName);
    }
}
