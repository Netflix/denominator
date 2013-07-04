package denominator.model.rdata;

import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code NS} (Name Server)
 * RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * NSData rdata = NSData.create(&quot;ns.foo.com.&quot;);
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class NSData extends LinkedHashMap<String, Object> {

    public static NSData create(String nsdname) {
        return new NSData(nsdname);
    }

    @ConstructorProperties("nsdname")
    NSData(String nsdname) {
        put("nsdname", checkNotNull(nsdname, "nsdname"));
    }

    /**
     * domain-name which specifies a host which should be authoritative for the
     * specified class and domain.
     * 
     * @since 1.3
     */
    public String nsdname() {
        return get("nsdname").toString();
    }

    private static final long serialVersionUID = 1L;
}
