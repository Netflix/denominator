package denominator.model.rdata;

import java.util.LinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code SPF} (Sender Policy Framework) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * import static denominator.model.rdata.SPFData.spf;
 * ...
 * SPFData rdata = spf("v=spf1 +mx a:colo.example.com/28 -all");
 * </pre>
 *
 * See <a href="http://tools.ietf.org/html/rfc4408#section-3.1.1">RFC 4408</a>
 */
public final class SPFData extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  SPFData(String txtdata) {
    checkArgument(checkNotNull(txtdata, "txtdata").length() <= 65535,
                  "txt data is limited to 65535");
    put("txtdata", txtdata);
  }

  public static SPFData create(String txtdata) {
    return new SPFData(txtdata);
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
