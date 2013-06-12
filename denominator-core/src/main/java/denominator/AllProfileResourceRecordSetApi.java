package denominator;


/**
 * @deprecated Will be removed in denominator 2.0. Please use
 *             {@link ReadOnlyResourceRecordSetApi}
 */
@Deprecated
public interface AllProfileResourceRecordSetApi extends ReadOnlyResourceRecordSetApi {
    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link ReadOnlyResourceRecordSetApi}
     */
    @Deprecated
    static interface Factory extends ReadOnlyResourceRecordSetApi.Factory {
        AllProfileResourceRecordSetApi create(String idOrName);
    }
}
