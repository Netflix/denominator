package denominator;

import java.util.Iterator;

import denominator.model.ResourceRecordSet;

public interface ResourceRecordSetApi extends Iterable<ResourceRecordSet<?>> {

  /**
   * Iterates across all basic record sets in the zone (those with no profile). Implementations are
   * lazy when possible.
   *
   * @return iterator which is lazy where possible
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  Iterator<ResourceRecordSet<?>> iterator();

  /**
   * a listing of all resource record sets which have the specified name.
   *
   * @return iterator which is lazy where possible, empty if there are no records with that name.
   * @throws IllegalArgumentException if the zone is not found.
   * @since 1.3
   */
  Iterator<ResourceRecordSet<?>> iterateByName(String name);

  /**
   * retrieve a resource record set by name and type.
   *
   * @param name {@link ResourceRecordSet#name() name} of the rrset
   * @param type {@link ResourceRecordSet#type() type} of the rrset
   * @return null unless a resource record exists with the same {@code name} and {@code type}
   * @throws IllegalArgumentException if the zone is not found.
   */
  ResourceRecordSet<?> getByNameAndType(String name, String type);

  /**
   * Idempotently replaces any existing records with {@link ResourceRecordSet#name() name} and
   * {@link ResourceRecordSet#type()} corresponding to {@code rrset}. If no records exists, they
   * will be added.
   *
   * <br> Example of replacing the {@code A} record set for {@code www.denominator.io.}:
   *
   * <pre>
   * rrsApi.put(a(&quot;www.denominator.io.&quot;, &quot;192.0.2.1&quot;));
   * </pre>
   *
   * @param rrset contains the {@code rdata} elements ensure exist. If {@link
   *              ResourceRecordSet#ttl() ttl} is not present, zone default is used.
   * @throws IllegalArgumentException if the zone is not found
   * @since 1.3
   */
  void put(ResourceRecordSet<?> rrset);

  /**
   * deletes a resource record set by name and type idempotently. This does not error if the record
   * set doesn't exist.
   *
   * @param name {@link ResourceRecordSet#name() name} of the rrset
   * @param type {@link ResourceRecordSet#type() type} of the rrset
   * @throws IllegalArgumentException if the zone is not found.
   */
  void deleteByNameAndType(String name, String type);

  interface Factory {

    ResourceRecordSetApi create(String id);
  }
}
