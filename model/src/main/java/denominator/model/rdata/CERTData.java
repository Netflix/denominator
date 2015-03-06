package denominator.model.rdata;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code CERT} (Certificate) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * CERTData rdata = CERTData.builder()
 *                          .format(12345)
 *                          .tag(1)
 *                          .algorithm(1)
 *                          .certificate("B33F").build()
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc4398.txt">RFC 4398</a>
 */
public final class CERTData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  CERTData(int format, int tag, int algorithm, String certificate) {
    checkArgument(format <= 0xFFFF, "format must be 0-65535");
    checkArgument(tag <= 0xFFFF, "tag must be 0-65535");
    checkArgument(algorithm <= 0xFF, "algorithm must be 0-255");
    checkNotNull(certificate, "certificate");
    put("format", format);
    put("tag", tag);
    put("algorithm", algorithm);
    put("certificate", certificate);
  }

  public static CERTData.Builder builder() {
    return new Builder();
  }

  /** {@code type} in the spec. This name avoids clashing on keywords. */
  public int format() {
    return Integer.class.cast(get("format"));
  }

  public int tag() {
    return Integer.class.cast(get("tag"));
  }

  public int algorithm() {
    return Integer.class.cast(get("algorithm"));
  }

  public String certificate() {
    return get("certificate").toString();
  }

  public CERTData.Builder toBuilder() {
    return builder().from(this);
  }

  public final static class Builder {

    private int format = -1;
    private int tag = -1;
    private int algorithm = -1;
    private String certificate;

    public CERTData.Builder format(int format) {
      this.format = format;
      return this;
    }

    public CERTData.Builder tag(int tag) {
      this.tag = tag;
      return this;
    }

    public CERTData.Builder algorithm(int algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public CERTData.Builder certificate(String certificate) {
      this.certificate = certificate;
      return this;
    }

    public CERTData build() {
      return new CERTData(format, tag, algorithm, certificate);
    }

    public CERTData.Builder from(CERTData in) {
        return format(in.format()).tag(in.tag()).algorithm(in.algorithm()).certificate(in.certificate());
    }
  }
}
