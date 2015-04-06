package denominator.ultradns;

import feign.FeignException;

class UltraDNSException extends FeignException {

  /**
   * System Error
   */
  static final int SYSTEM_ERROR = 9999;
  /**
   * Zone does not exist in the system.
   */
  static final int ZONE_NOT_FOUND = 1801;
  /**
   * Zone already exists in the system.
   */
  static final int ZONE_ALREADY_EXISTS = 1802;
  /**
   * No resource record with GUID found in the system.
   */
  static final int RESOURCE_RECORD_NOT_FOUND = 2103;
  /**
   * Resource record exists with the same name and type.
   */
  static final int RESOURCE_RECORD_ALREADY_EXISTS = 2111;

  // there are 51002 potential codes. These are the ones we are handling.
  /**
   * No Pool or Multiple pools of same type exists for the PoolName
   */
  static final int DIRECTIONALPOOL_NOT_FOUND = 2142;
  /**
   * Invalid zone name
   */
  static final int INVALID_ZONE_NAME = 2507;
  /**
   * Directional Pool Record does not exist in the system
   */
  static final int DIRECTIONALPOOL_RECORD_NOT_FOUND = 2705;
  /**
   * Pool does not exist in the system.
   */
  static final int POOL_NOT_FOUND = 2911;
  /**
   * Pool already created for the given rrGUID.
   */
  static final int POOL_ALREADY_EXISTS = 2912;
  /**
   * Group does not exist.
   */
  static final int GROUP_NOT_FOUND = 4003;
  /**
   * Directional feature not Enabled or Directional migration is not done.
   */
  static final int DIRECTIONAL_NOT_ENABLED = 4006;
  /**
   * Resource Record already exists.
   */
  static final int POOL_RECORD_ALREADY_EXISTS = 4009;
  private static final long serialVersionUID = 1L;
  private final int code;

  UltraDNSException(String message, int code) {
    super(message);
    this.code = code;
  }

  /**
   * The error code. ex {@code 1801}
   */
  public int code() {
    return code;
  }

}
