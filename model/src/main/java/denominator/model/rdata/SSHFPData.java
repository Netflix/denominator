package denominator.model.rdata;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code SSHFP} (SSH Fingerprint) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * SSHFPData rdata = SSHFPData.builder().algorithm(2).fptype(1).fingerprint(&quot;123456789abcdef67890123456789abcdef67890&quot;)
 *         .build();
 * // or shortcut
 * SSHFPData rdata = SSHFPData.createDSA(&quot;123456789abcdef67890123456789abcdef67890&quot;);
 * </pre>
 *
 * See <a href="http://www.rfc-editor.org/rfc/rfc4255.txt">RFC 4255</a>
 */
public final class SSHFPData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  SSHFPData(int algorithm, int fptype, String fingerprint) {
    checkArgument(algorithm >= 0, "algorithm of %s must be unsigned", fingerprint);
    checkArgument(fptype >= 0, "fptype of %s must be unsigned", fingerprint);
    checkNotNull(fingerprint, "fingerprint");
    put("algorithm", algorithm);
    put("fptype", fptype);
    put("fingerprint", fingerprint);
  }

  /**
   * @param fingerprint {@code DSA} {@code SHA-1} fingerprint
   */
  public static SSHFPData createDSA(String fingerprint) {
    return builder().algorithm(2).fptype(1).fingerprint(fingerprint).build();
  }

  /**
   * @param fingerprint {@code RSA} {@code SHA-1} fingerprint
   */
  public static SSHFPData createRSA(String fingerprint) {
    return builder().algorithm(1).fptype(1).fingerprint(fingerprint).build();
  }

  public static SSHFPData.Builder builder() {
    return new Builder();
  }

  /**
   * This algorithm number octet describes the algorithm of the public key.
   *
   * @return most often {@code 1} for {@code RSA} or {@code 2} for {@code DSA} .
   * @since 1.3
   */
  public int algorithm() {
    return Integer.class.cast(get("algorithm"));
  }

  /**
   * The fingerprint fptype octet describes the message-digest algorithm used to calculate the
   * fingerprint of the public key.
   *
   * @return most often {@code 1} for {@code SHA-1}
   * @since 1.3
   */
  public int fptype() {
    return Integer.class.cast(get("fptype"));
  }

  /**
   * The fingerprint calculated over the public key blob.
   *
   * @since 1.3
   */
  public String fingerprint() {
    return get("fingerprint").toString();
  }

  public final static class Builder {

    private int algorithm;
    private int fptype;
    private String fingerprint;

    /**
     * @see SSHFPData#algorithm()
     */
    public SSHFPData.Builder algorithm(int algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    /**
     * @see SSHFPData#fptype()
     */
    public SSHFPData.Builder fptype(int fptype) {
      this.fptype = fptype;
      return this;
    }

    /**
     * @see SSHFPData#fingerprint()
     */
    public SSHFPData.Builder fingerprint(String fingerprint) {
      this.fingerprint = fingerprint;
      return this;
    }

    public SSHFPData build() {
      return new SSHFPData(algorithm, fptype, fingerprint);
    }
  }
}
