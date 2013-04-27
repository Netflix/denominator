package denominator;

import com.google.common.annotations.Beta;

import denominator.model.ResourceRecordSet;

/**
 * list operations that apply to record sets regardless of
 * {@link ResourceRecordSet#getProfiles() profile}.
 */
@Beta
public interface AllProfileResourceRecordSetApi extends ReadOnlyResourceRecordSetApi {

    static interface Factory {
        AllProfileResourceRecordSetApi create(String zoneName);
    }
}
