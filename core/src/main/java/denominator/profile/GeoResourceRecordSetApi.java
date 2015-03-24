package denominator.profile;

import java.util.Collection;
import java.util.Map;

import denominator.QualifiedResourceRecordSetApi;

/**
 * list operations are filtered to only include those which are geo record sets.
 */
public interface GeoResourceRecordSetApi extends QualifiedResourceRecordSetApi {

  /**
   * retrieve an organized list of regions by region. It is often the case that the keys correlate
   * to UN or otherwise defined regions such as {@code North America}. However, this can also
   * include special case keys, such as {@code Fallback} and {@code Anonymous Proxy}. <br> ex.
   *
   * <pre>
   * {
   *     "States and Provinces: Canada": ["ab", "bc", "mb", "nb", "nl", "nt", "ns", "nu", "on",
   * "pe", "qc", "sk", "yt"],
   *     "Fallback": ["@@"],
   *     "Anonymous Proxy": ["A1"],
   *     "Other Country": ["O1"],
   *     "Satellite Provider": ["A2"]
   * }
   * </pre>
   *
   * <br> <br> <b>Note</b><br>
   *
   * The values of this are not guaranteed portable across providers.
   *
   * @since 1.3
   */
  Map<String, Collection<String>> supportedRegions();

  static interface Factory extends QualifiedResourceRecordSetApi.Factory {

    /**
     * @return null if this feature isn't supported on the provider.
     */
    @Override
    GeoResourceRecordSetApi create(String id);
  }
}
