package denominator.dynect;

import java.util.List;

import feign.FeignException;

import static java.lang.String.format;

public class DynECTException extends FeignException {

  private static final long serialVersionUID = 1L;
  private final String status;
  private final List<Message> messages;

  DynECTException(String status, List<Message> messages) {
    super(format("status %s: %s", status, messages));
    this.status = status;
    this.messages = messages;
  }

  /**
   * For example: {@code running}.
   */
  public String status() {
    return status;
  }

  /**
   * Messages corresponding to the changes.
   */
  public List<Message> messages() {
    return messages;
  }

  public static class Message {

    String code;
    String info;

    /**
     * nullable
     */
    public String code() {
      return code;
    }

    public String info() {
      return info;
    }

    @Override
    public String toString() {
      if (code != null) {
        return code + ": " + info;
      }
      return info;
    }
  }
}
