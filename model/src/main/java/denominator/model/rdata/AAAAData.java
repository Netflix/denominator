package denominator.model.rdata;

import java.util.LinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code AAAA} (Address) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * AAAAData rdata = AAAAData.create(&quot;1234:ab00:ff00::6b14:abcd&quot;);
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc3596.txt">RFC 3596</a>
 */
public final class AAAAData extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  AAAAData(String address) {
    checkNotNull(address, "address");
    checkArgument(address.indexOf(':') != -1, "%s should be a ipv6 address", address);
    put("address", address);
  }

  /**
   * @param ipv6address valid ipv6 address. ex. {@code 1234:ab00:ff00::6b14:abcd}
   * @throws IllegalArgumentException if the address is malformed or not ipv6
   */
  public static AAAAData create(String ipv6address) throws IllegalArgumentException {
    return new AAAAData(ipv6address);
  }

  /**
   * a 128 bit IPv6 address
   *
   * @since 1.3
   */
  public String address() {
    return get("address").toString();
  }
}
