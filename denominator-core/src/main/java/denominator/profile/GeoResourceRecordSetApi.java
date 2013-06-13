package denominator.profile;

import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import denominator.Provider;
import denominator.QualifiedResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;

/**
 * list operations are filtered to only include those which are geo record sets.
 */
@Beta
public interface GeoResourceRecordSetApi extends QualifiedResourceRecordSetApi {

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link Provider#profileToRecordTypes()} with the {@code geo} key.
     */
    @Deprecated
    Set<String> getSupportedTypes();

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #supportedRegions()}
     */
    @Deprecated
    Multimap<String, String> getSupportedRegions();

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

    /**
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link denominator.ReadOnlyResourceRecordSetApi#getByNameTypeAndQualifier(String, String, String)}
     */
    @Deprecated
    Optional<ResourceRecordSet<?>> getByNameTypeAndGroup(String name, String type, String group);

    /**
     * Ensures the supplied {@code regions} are uniform for all record sets with
     * the supplied {@link ResourceRecordSet#name() name},
     * {@link ResourceRecordSet#type() type}, and
     * {@link ResourceRecordSet#qualifier() qualifier}. Returns without error if
     * there are no record sets of the specified name, type, and qualifier.
     * 
     * @param regions
     *            corresponds to {@link Geo#regions() regions} you want this
     *            {@code qualifier} to represent. Should be a sub-map of
     *            {@link #supportedRegions()}.
     * @param name
     *            {@link ResourceRecordSet#name() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#type() type} of the rrset
     * @param qualifier
     *            {@link ResourceRecordSet#qualifier() qualifier} of the rrset
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #put(ResourceRecordSet)}
     */
    @Deprecated
    void applyRegionsToNameTypeAndGroup(Multimap<String, String> regions, String name, String type, String group);

    /**
     * Ensures the supplied {@code ttl} is uniform for all record sets with the
     * supplied {@link ResourceRecordSet#name() name},
     * {@link ResourceRecordSet#type() type}, and
     * {@link ResourceRecordSet#qualifier() qualifier} . Returns without error
     * if there are no record sets of the specified name, type, and qualifier.
     * 
     * @param ttl
     *            ttl to apply to all records in seconds
     * @param name
     *            {@link ResourceRecordSet#name() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#type() type} of the rrset
     * @param qualifier
     *            {@link ResourceRecordSet#qualifier() qualifier} of the rrset
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #put(ResourceRecordSet)}
     */
    @Deprecated
    void applyTTLToNameTypeAndGroup(int ttl, String name, String type, String group);

    static interface Factory extends QualifiedResourceRecordSetApi.Factory {
        Optional<GeoResourceRecordSetApi> create(String idOrName);
    }
}
