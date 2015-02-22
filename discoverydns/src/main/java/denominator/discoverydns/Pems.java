package denominator.discoverydns;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

/**
 * Based on: https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/util/io/pem/PemReader.java
 */
final class Pems {

  private static final String BEGIN = "-----BEGIN ";
  private static final String END = "-----END ";

  static Certificate readCertificate(String pem) throws IOException {
    try {
      CertificateFactory certFactory = CertificateFactory.getInstance("X509");
      String type = getType(pem);
      if ("CERTIFICATE".equals(type)) {
        byte[] certBytes = getBytes(pem);
        Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
        return cert;
      }
      return null;
    } catch (GeneralSecurityException e) {
      throw new IOException("Could not read certificate");
    }
  }

  static PrivateKey readPrivateKey(String pem) throws IOException {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      String type = getType(pem);
      byte[] keyBytes = getBytes(pem);
      KeySpec keySpec = null;
      if ("RSA PRIVATE KEY".equals(type)) {
        keySpec = getRSAKeySpec(keyBytes);
      } else {
        keySpec = new PKCS8EncodedKeySpec(keyBytes);
      }
      PrivateKey key = keyFactory.generatePrivate(keySpec);
      return key;
    } catch (GeneralSecurityException e) {
      throw new IOException("Could not read key");
    }
  }

  static byte[] getBytes(String pem) throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(pem));
    try {
      String line = reader.readLine();

      // Got nothing
      if (line == null) {
        return null;
      }

      // Skip to start
      while (line != null && !line.startsWith(BEGIN)) {
        line = reader.readLine();
      }

      line = line.substring(BEGIN.length());
      int index = line.indexOf('-');

      if (index > 0) {
        StringBuffer buf = new StringBuffer();

        while ((line = reader.readLine()) != null) {
          if (line.indexOf(":") >= 0) {
            continue;
          }

          if (line.indexOf(END) != -1) {
            break;
          }

          buf.append(line.trim());
        }

        if (line == null) {
          throw new IOException(END + " not found");
        }

        return base64(buf.toString());
      }

      return null;
    } finally {
      reader.close();
    }
  }

  static String getType(String pem) throws IOException {
    if (pem.contains("ENCRYPTED")) {
      throw new IOException("Encrypted content is not currently supported");
    }

    BufferedReader reader = new BufferedReader(new StringReader(pem));
    try {
      String line = reader.readLine();

      // Got nothing
      if (line == null) {
        return null;
      }

      // Skip to start
      while (line != null && !line.startsWith(BEGIN)) {
        line = reader.readLine();
      }

      line = line.substring(BEGIN.length());
      int index = line.indexOf('-');
      String type = line.substring(0, index);
      return type;
    } finally {
      reader.close();
    }
  }

  private static RSAPrivateCrtKeySpec getRSAKeySpec(byte[] keyBytes) throws IOException {
    Pems.DerParser parser = new DerParser(keyBytes);

    Asn1Object sequence = parser.read();
    if (sequence.getType() != DerParser.SEQUENCE) {
      throw new IOException("Invalid DER: not a sequence");
    }

    // Parse inside the sequence
    parser = sequence.getParser();

    parser.read(); // Skip version
    BigInteger modulus = parser.read().getInteger();
    BigInteger Exp = parser.read().getInteger();
    BigInteger privateExp = parser.read().getInteger();
    BigInteger prime1 = parser.read().getInteger();
    BigInteger prime2 = parser.read().getInteger();
    BigInteger exp1 = parser.read().getInteger();
    BigInteger exp2 = parser.read().getInteger();
    BigInteger crtCoef = parser.read().getInteger();

    RSAPrivateCrtKeySpec
        keySpec =
        new RSAPrivateCrtKeySpec(modulus, Exp, privateExp, prime1, prime2, exp1, exp2,
                                 crtCoef);

    return keySpec;
  }

  /**
   * Based on: http://oauth.googlecode.com/svn/code/java/jmeter/jmeter/src/main/java/org/apache/jmeter/protocol/oauth/sampler/PrivateKeyReader.java
   */
  private static class DerParser {

    // Constructed Flag
    final static int CONSTRUCTED = 0x20;

    // Tag and data types
    final static int INTEGER = 0x02;
    final static int SEQUENCE = 0x10;

    private InputStream in;

    /**
     * Create a new DER decoder from an input stream.
     *
     * @param in The DER encoded stream
     */
    DerParser(InputStream in) throws IOException {
      this.in = in;
    }

    /** Creates a new DER decoder from a byte array. */
    DerParser(byte[] encodedBytes) throws IOException {
      this(new ByteArrayInputStream(encodedBytes));
    }

    /**
     * Read next object. If it's constructed, the value holds encoded content and it should be
     * parsed by a new parser from <code>Asn1Object.getParser</code>.
     *
     * @return A object
     */
    Asn1Object read() throws IOException {
      int tag = in.read();

      if (tag == -1) {
        throw new IOException("Invalid DER: stream too short, missing tag");
      }

      int length = getLength();

      byte[] value = new byte[length];
      int n = in.read(value);
      if (n < length) {
        throw new IOException("Invalid DER: stream too short, missing value");
      }

      Asn1Object o = new Asn1Object(tag, value);

      return o;
    }

    /**
     * Decode the length of the field. Can only support length encoding up to 4 octets.
     *
     * <p/> In BER/DER encoding, length can be encoded in 2 forms, <ul> <li>Short form. One octet.
     * Bit 8 has value "0" and bits 7-1 give the length. <li>Long form. Two to 127 octets (only 4 is
     * supported here). Bit 8 of first octet has value "1" and bits 7-1 give the number of
     * additional length octets. Second and following octets give the length, base 256, most
     * significant digit first. </ul>
     *
     * @return The length as integer
     */
    private int getLength() throws IOException {

      int i = in.read();
      if (i == -1) {
        throw new IOException("Invalid DER: length missing");
      }

      // A single byte short length
      if ((i & ~0x7F) == 0) {
        return i;
      }

      int num = i & 0x7F;

      // We can't handle length longer than 4 bytes
      if (i >= 0xFF || num > 4) {
        throw new IOException("Invalid DER: length field too big (" + i + ")");
      }

      byte[] bytes = new byte[num];
      int n = in.read(bytes);
      if (n < num) {
        throw new IOException("Invalid DER: length too short");
      }

      return new BigInteger(1, bytes).intValue();
    }
  }

  /**
   * Based on: http://oauth.googlecode.com/svn/code/java/jmeter/jmeter/src/main/java/org/apache/jmeter/protocol/oauth/sampler/PrivateKeyReader.java
   */
  private static class Asn1Object {

    private final int type;
    private final byte[] value;
    private final int tag;

    /**
     * Construct a ASN.1 TLV. The TLV could be either a constructed or primitive entity.
     *
     * <p/> The first byte in DER encoding is made of following fields,
     *
     * <pre>
     * -------------------------------------------------
     * |Bit 8|Bit 7|Bit 6|Bit 5|Bit 4|Bit 3|Bit 2|Bit 1|
     * -------------------------------------------------
     * |  Class    | CF  |     +      Type             |
     * -------------------------------------------------
     * </pre>
     *
     * <ul> <li>Class: Universal, Application, Context or Private <li>CF: Constructed flag. If 1,
     * the field is constructed. <li>Type: This is actually called tag in ASN.1. It indicates data
     * type (Integer, String) or a construct (sequence, choice, set). </ul>
     *
     * @param tag    Tag or Identifier
     * @param value  Encoded octet string for the field.
     */
    Asn1Object(int tag, byte[] value) {
      this.tag = tag;
      this.type = tag & 0x1F;
      this.value = value;
    }

    int getType() {
      return type;
    }

    boolean isConstructed() {
      return (tag & DerParser.CONSTRUCTED) == DerParser.CONSTRUCTED;
    }

    /**
     * For constructed field, return a parser for its content.
     *
     * @return A parser for the construct.
     */
    DerParser getParser() throws IOException {
      if (!isConstructed()) {
        throw new IOException("Invalid DER: can't parse primitive entity");
      }

      return new DerParser(value);
    }

    /**
     * Get the value as integer
     *
     * @return BigInteger
     */
    private BigInteger getInteger() throws IOException {
      if (type != DerParser.INTEGER) {
        throw new IOException("Invalid DER: object is not integer");
      }

      return new BigInteger(value);
    }
  }

  /**
   * copied from <a href="https://github.com/square/okio/blob/master/okio/src/main/java/okio/Base64.java">okio</a>
   *
   * @author Alexander Y. Kleymenov
   */
  public static byte[] base64(String in) {
    // Ignore trailing '=' padding and whitespace from the input.
    int limit = in.length();
    for (; limit > 0; limit--) {
      char c = in.charAt(limit - 1);
      if (c != '=' && c != '\n' && c != '\r' && c != ' ' && c != '\t') {
        break;
      }
    }

    // If the input includes whitespace, this output array will be longer than necessary.
    byte[] out = new byte[(int) (limit * 6L / 8L)];
    int outCount = 0;
    int inCount = 0;

    int word = 0;
    for (int pos = 0; pos < limit; pos++) {
      char c = in.charAt(pos);

      int bits;
      if (c >= 'A' && c <= 'Z') {
        // char ASCII value
        //  A    65    0
        //  Z    90    25 (ASCII - 65)
        bits = c - 65;
      } else if (c >= 'a' && c <= 'z') {
        // char ASCII value
        //  a    97    26
        //  z    122   51 (ASCII - 71)
        bits = c - 71;
      } else if (c >= '0' && c <= '9') {
        // char ASCII value
        //  0    48    52
        //  9    57    61 (ASCII + 4)
        bits = c + 4;
      } else if (c == '+') {
        bits = 62;
      } else if (c == '/') {
        bits = 63;
      } else if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
        continue;
      } else {
        return null;
      }

      // Append this char's 6 bits to the word.
      word = (word << 6) | (byte) bits;

      // For every 4 chars of input, we accumulate 24 bits of output. Emit 3 bytes.
      inCount++;
      if (inCount % 4 == 0) {
        out[outCount++] = (byte) (word >> 16);
        out[outCount++] = (byte) (word >> 8);
        out[outCount++] = (byte) word;
      }
    }

    int lastWordChars = inCount % 4;
    if (lastWordChars == 1) {
      // We read 1 char followed by "===". But 6 bits is a truncated byte! Fail.
      return null;
    } else if (lastWordChars == 2) {
      // We read 2 chars followed by "==". Emit 1 byte with 8 of those 12 bits.
      word = word << 12;
      out[outCount++] = (byte) (word >> 16);
    } else if (lastWordChars == 3) {
      // We read 3 chars, followed by "=". Emit 2 bytes for 16 of those 18 bits.
      word = word << 6;
      out[outCount++] = (byte) (word >> 16);
      out[outCount++] = (byte) (word >> 8);
    }

    // If we sized our out array perfectly, we're done.
    if (outCount == out.length) {
      return out;
    }

    // Copy the decoded bytes to a new, right-sized array.
    byte[] prefix = new byte[outCount];
    System.arraycopy(out, 0, prefix, 0, outCount);
    return prefix;
  }
}
