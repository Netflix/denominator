package denominator.model.rdata;

import java.util.LinkedHashMap;

import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code PTR} (Pointer) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * PTRData rdata = PTRData.create(&quot;ptr.foo.com.&quot;);
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public final class PTRData extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  PTRData(String ptrdname) {
    put("ptrdname", checkNotNull(ptrdname, "ptrdname"));
  }

  public static PTRData create(String ptrdname) {
    return new PTRData(ptrdname);
  }

  /**
   * domain-name which points to some location in the domain name space.
   *
   * @since 1.3
   */
  public String ptrdname() {
    return get("ptrdname").toString();
  }
}
