package denominator.profile;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import denominator.QualifiedResourceRecordSetApi;

/**
 * list operations are filtered to only include those which are geo record sets.
 */
@Beta
public interface GeoResourceRecordSetApi extends QualifiedResourceRecordSetApi {

    /**
     * retrieve an organized list of regions by region. It is often the case
     * that the keys correlate to UN or otherwise defined regions such as
     * {@code North America}. However, this can also include special case keys,
     * such as {@code Fallback} and {@code Anonymous Proxy}.
     * <p/>
     * ex.
     * 
     * <pre>
     * {
     *     "States and Provinces: Canada": ["ab", "bc", "mb", "nb", "nl", "nt", "ns", "nu", "on", "pe", "qc", "sk", "yt"],
     *     "Fallback": ["@@"],
     *     "Anonymous Proxy": ["A1"],
     *     "Other Country": ["O1"],
     *     "Satellite Provider": ["A2"]
     * }
     * </pre>
     * 
     * <h4>Note</h4>
     * 
     * The values of this are not guaranteed portable across providers.
     * 
     * @since 1.3
     */
    Multimap<String, String> supportedRegions();

    static interface Factory extends QualifiedResourceRecordSetApi.Factory {
        Optional<GeoResourceRecordSetApi> create(String idOrName);
    }
}
