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
 *                          .certType(12345)
 *                          .keyTag(1)
 *                          .algorithm(1)
 *                          .cert("B33F").build()
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc4398.txt">RFC 4398</a>
 */
public final class CERTData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  CERTData(int certType, int keyTag, int algorithm, String cert) {
    checkArgument(certType <= 0xFFFF, "certType must be 0-65535");
    checkArgument(keyTag <= 0xFFFF, "keyTag must be 0-65535");
    checkArgument(algorithm <= 0xFF, "algorithm must be 0-255");
    checkNotNull(cert, "cert");
    put("certType", certType);
    put("keyTag", keyTag);
    put("algorithm", algorithm);
    put("cert", cert);
  }

  public static CERTData.Builder builder() {
    return new Builder();
  }

  public int certType() {
    return Integer.class.cast(get("certType"));
  }

  public int keyTag() {
    return Integer.class.cast(get("keyTag"));
  }

  public int algorithm() {
    return Integer.class.cast(get("algorithm"));
  }

  public String cert() {
    return get("cert").toString();
  }

  public CERTData.Builder toBuilder() {
    return builder().from(this);
  }

  public final static class Builder {

    private int certType = -1;
    private int keyTag = -1;
    private int algorithm = -1;
    private String cert;

    public CERTData.Builder certType(int certType) {
      this.certType = certType;
      return this;
    }

    public CERTData.Builder keyTag(int keyTag) {
      this.keyTag = keyTag;
      return this;
    }

    public CERTData.Builder algorithm(int algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public CERTData.Builder cert(String cert) {
      this.cert = cert;
      return this;
    }

    public CERTData build() {
      return new CERTData(certType, keyTag, algorithm, cert);
    }

    public CERTData.Builder from(CERTData in) {
      return this.certType(in.certType())
          .keyTag(in.keyTag())
          .algorithm(in.algorithm())
          .cert(in.cert());
    }
  }
}
