package denominator.verisigndns;

import feign.FeignException;

final class VerisignDnsException extends FeignException {

  static final int SYSTEM_ERROR = -1;
  static final int DOMAIN_NOT_FOUND = 1;
  static final int DOMAIN_ALREADY_EXISTS = 2;

  private static final long serialVersionUID = 1L;
  private final int code;
  private final String description;

  VerisignDnsException(String message, int code, String description) {
    super(message);
    this.code = code;
    this.description = description;
  }

  public int code() {
    return code;
  }

  public String description() {
    return description;
  }
}
