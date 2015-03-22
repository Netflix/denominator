package denominator;

import denominator.model.ResourceRecordSet;

/**
 * Write operations for {@link ResourceRecordSet record sets} who have a {@link
 * ResourceRecordSet#qualifier()}.
 *
 * @since 1.3
 */
public interface QualifiedResourceRecordSetApi extends ReadOnlyResourceRecordSetApi {

  /**
   * Idempotently replaces any existing records with {@link ResourceRecordSet#name() name}, {@link
   * ResourceRecordSet#type() type}, and {@link ResourceRecordSet#qualifier() qualifier}
   * corresponding to {@code rrset}. If no records exist, they will be added.
   *
   * <br> Example of replacing the {@code A} record set for {@code www.denominator.io.} qualified as
   * {@code US-West}:
   *
   * <pre>
   * rrsApi.put(ResourceRecordSet.&lt;AData&gt; builder().name(&quot;www.denominator.io.&quot;).type(&quot;A&quot;).qualifier(&quot;US-West&quot;).ttl(3600)
   *         .add(AData.create(&quot;192.0.2.1&quot;)).build());
   * </pre>
   *
   * @param rrset contains the {@code rdata} elements ensure exist. If {@link
   *              ResourceRecordSet#ttl() ttl} is not present, zone default is used.
   * @throws IllegalArgumentException if the zone is not found.
   */
  void put(ResourceRecordSet<?> rrset);

  /**
   * Idempotently deletes a resource record set by {@link ResourceRecordSet#name() name}, {@link
   * ResourceRecordSet#type() type}, and {@link ResourceRecordSet#qualifier() qualifier}. This does
   * not error if the record set doesn't exist.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param qualifier {@link ResourceRecordSet#qualifier() qualifier} of the rrset
   * @throws IllegalArgumentException if the zone is not found.
   */
  void deleteByNameTypeAndQualifier(String name, String type, String qualifier);

  static interface Factory {

    /**
     * @return null if this feature isn't supported on the provider.
     */
    QualifiedResourceRecordSetApi create(String id);
  }
}
