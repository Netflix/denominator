package denominator.model.rdata;

import java.util.LinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code TXT} (Text) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * TXTData rdata = TXTData.create(&quot;=spf1 ip4:192.0.2.1/24 ip4:198.51.100.1/24 -all&quot;);
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public final class TXTData extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  TXTData(String txtdata) {
    checkArgument(checkNotNull(txtdata, "txtdata").length() <= 65535,
                  "txt data is limited to 65535");
    put("txtdata", txtdata);
  }

  public static TXTData create(String txtdata) {
    return new TXTData(txtdata);
  }

  /**
   * One or more character-strings.
   *
   * @since 1.3
   */
  public String txtdata() {
    return get("txtdata").toString();
  }
}
