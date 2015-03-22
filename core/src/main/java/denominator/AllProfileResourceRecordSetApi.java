package denominator;

import denominator.model.ResourceRecordSet;

/**
 * Controls all resource records in the provider
 */
public interface AllProfileResourceRecordSetApi extends QualifiedResourceRecordSetApi {

  /**
   * Idempotently deletes all resource record sets with the specified {@link
   * ResourceRecordSet#name() name} and {@link ResourceRecordSet#type()}. This does not error if no
   * record sets match.
   *
   * @param name {@link ResourceRecordSet#name() name} of the rrset
   * @param type {@link ResourceRecordSet#type() type} of the rrset
   * @throws IllegalArgumentException if the zone is not found.
   */
  void deleteByNameAndType(String name, String type);

  static interface Factory {

    AllProfileResourceRecordSetApi create(String id);
  }
}
