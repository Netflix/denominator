package denominator.route53;

import java.util.List;

import feign.FeignException;

import static java.lang.String.format;

/**
 * See <a href= "http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html#API_ChangeResourceRecordSets_ExampleErrors"
 * >docs</a>
 */
public class InvalidChangeBatchException extends FeignException {

  private static final long serialVersionUID = 1L;
  private final List<String> messages;

  InvalidChangeBatchException(String methodKey, List<String> messages) {
    super(format("%s failed with errors %s", methodKey, messages));
    this.messages = messages;
  }

  /**
   * Messages corresponding to the changes.
   */
  public List<String> messages() {
    return messages;
  }
}
