package denominator.route53;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Provider;

import denominator.Credentials;

import static java.util.Locale.US;
import static denominator.common.Preconditions.checkNotNull;

// http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/RESTAuthentication.html
final class InvalidatableAuthenticationHeadersProvider {

  private static final String
      AUTH_FORMAT =
      "AWS3-HTTPS AWSAccessKeyId=%s,Algorithm=HmacSHA256,Signature=%s";
  private static final String HMACSHA256 = "HmacSHA256";
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final SimpleDateFormat
      RFC1123 =
      new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss Z", US);
  private final javax.inject.Provider<Credentials> credentials;
  private final long durationMillis;
  transient volatile String lastKeystoneUrl;
  transient volatile int lastCredentialsHashCode;
  // The special value 0 means "not yet initialized".
  transient volatile long expirationMillis;
  // "value" does not need to be volatile; visibility piggy-backs
  // on above
  transient Map<String, String> value;

  @Inject
  InvalidatableAuthenticationHeadersProvider(Provider<Credentials> credentials) {
    this.credentials = credentials;
    // variance allowed is 5 minutes, so only re-hashing every 1m.
    this.durationMillis = TimeUnit.MINUTES.toMillis(1);
  }

  private static synchronized String date() {
    return RFC1123.format(new Date());
  }

  public void invalidate() {
    expirationMillis = 0;
  }

  public Map<String, String> get() {
    long currentTime = System.currentTimeMillis();
    Credentials currentCreds = credentials.get();
    if (needsRefresh(currentTime, currentCreds)) {
      synchronized (this) {
        if (needsRefresh(currentTime, currentCreds)) {
          lastCredentialsHashCode = currentCreds.hashCode();
          Map<String, String> t = auth(currentCreds);
          value = t;
          expirationMillis = currentTime + durationMillis;
          return t;
        }
      }
    }
    return value;
  }

  private boolean needsRefresh(long currentTime, Credentials currentCreds) {
    return expirationMillis == 0 || currentTime - expirationMillis >= 0
           || currentCreds.hashCode() != lastCredentialsHashCode;
  }

  Map<String, String> auth(Credentials currentCreds) {
    String accessKey;
    String secretKey;
    String token;
    if (currentCreds instanceof List) {
      @SuppressWarnings("unchecked")
      List<Object> listCreds = (List<Object>) currentCreds;
      accessKey = listCreds.get(0).toString();
      secretKey = listCreds.get(1).toString();
      token = listCreds.size() == 3 ? listCreds.get(2).toString() : null;
    } else if (currentCreds instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mapCreds = (Map<String, Object>) currentCreds;
      accessKey = checkNotNull(mapCreds.get("accessKey"), "accessKey").toString();
      secretKey = checkNotNull(mapCreds.get("secretKey"), "secretKey").toString();
      token = mapCreds.containsKey("sessionToken") ? mapCreds.get("sessionToken").toString() : null;
    } else {
      throw new IllegalArgumentException("Unsupported credential type: " + currentCreds);
    }

    String rfc1123Date = date();
    String signature = sign(rfc1123Date, secretKey);
    String auth = String.format(AUTH_FORMAT, accessKey, signature);

    Map<String, String> canContainNull = new LinkedHashMap<String, String>();
    canContainNull.put("Date", rfc1123Date);
    canContainNull.put("X-Amzn-Authorization", auth);
    // will remove if token is not set
    canContainNull.put("X-Amz-Security-Token", token);
    return canContainNull;
  }

  String sign(String rfc1123Date, String secretKey) {
    try {
      Mac mac = Mac.getInstance(HMACSHA256);
      mac.init(new SecretKeySpec(secretKey.getBytes(UTF_8), HMACSHA256));
      byte[] result = mac.doFinal(rfc1123Date.getBytes(UTF_8));
      return base64(result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * copied from <a href="https://github.com/square/okio/blob/master/okio/src/main/java/okio/Base64.java">okio</a>
   *
   * @author Alexander Y. Kleymenov
   */
  private static String base64(byte[] in) {
    int length = (in.length + 2) * 4 / 3;
    byte[] out = new byte[length];
    int index = 0, end = in.length - in.length % 3;
    for (int i = 0; i < end; i += 3) {
      out[index++] = MAP[(in[i] & 0xff) >> 2];
      out[index++] = MAP[((in[i] & 0x03) << 4) | ((in[i + 1] & 0xff) >> 4)];
      out[index++] = MAP[((in[i + 1] & 0x0f) << 2) | ((in[i + 2] & 0xff) >> 6)];
      out[index++] = MAP[(in[i + 2] & 0x3f)];
    }
    switch (in.length % 3) {
      case 1:
        out[index++] = MAP[(in[end] & 0xff) >> 2];
        out[index++] = MAP[(in[end] & 0x03) << 4];
        out[index++] = '=';
        out[index++] = '=';
        break;
      case 2:
        out[index++] = MAP[(in[end] & 0xff) >> 2];
        out[index++] = MAP[((in[end] & 0x03) << 4) | ((in[end + 1] & 0xff) >> 4)];
        out[index++] = MAP[((in[end + 1] & 0x0f) << 2)];
        out[index++] = '=';
        break;
    }
    try {
      return new String(out, 0, index, "US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private static final byte[] MAP = new byte[]{
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
      'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
      'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
      '5', '6', '7', '8', '9', '+', '/'
  };
}
