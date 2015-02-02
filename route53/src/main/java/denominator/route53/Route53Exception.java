package denominator.route53;

import feign.FeignException;

class Route53Exception extends FeignException {

  private static final long serialVersionUID = 1L;
  private final String code;

  Route53Exception(String message, String code) {
    super(message);
    this.code = code;
  }

  /**
   * The error code. ex {@code InvalidInput}
   */
  public String code() {
    return code;
  }
}
