package denominator.model.rdata;

import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code PTR} (Pointer) RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * PTRData rdata = PTRData.create(&quot;ptr.foo.com.&quot;);
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class PTRData extends LinkedHashMap<String, Object> {

    public static PTRData create(String ptrdname) {
        return new PTRData(ptrdname);
    }

    @ConstructorProperties("ptrdname")
    PTRData(String ptrdname) {
        put("ptrdname", checkNotNull(ptrdname, "ptrdname"));
    }

    /**
     * domain-name which points to some location in the domain name space.
     * 
     * @since 1.3
     */
    public String ptrdname() {
        return get("ptrdname").toString();
    }

    private static final long serialVersionUID = 1L;
}
