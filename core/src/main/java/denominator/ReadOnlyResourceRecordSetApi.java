package denominator;

import java.util.Iterator;

import denominator.model.ResourceRecordSet;

public interface ReadOnlyResourceRecordSetApi extends Iterable<ResourceRecordSet<?>> {

  /**
   * Iterates across all record sets in the zone. Implementations are lazy when possible.
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
   * a listing of all resource record sets by name and type.
   *
   * @param name {@link ResourceRecordSet#name() name} of the rrset
   * @param type {@link ResourceRecordSet#type() type} of the rrset
   * @return iterator which is lazy where possible, empty if there are no records with that name.
   * @throws IllegalArgumentException if the zone is not found.
   * @since 1.3
   */
  Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type);

  /**
   * retrieve a resource record set by name, type, and qualifier
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param qualifier {@link ResourceRecordSet#qualifier() qualifier} of the rrset
   * @return null unless a resource record exists with the same {@code name}, {@code type}, and
   * {@code qualifier}
   * @throws IllegalArgumentException if the zone is not found.
   */
  ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier);
}
