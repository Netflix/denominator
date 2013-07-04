package denominator.model.rdata;

import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code CNAME} (Canonical
 * Name) RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * CNAMEData rdata = CNAMEData.create(&quot;cname.foo.com.&quot;);
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class CNAMEData extends LinkedHashMap<String, Object> {

    public static CNAMEData create(String cname) {
        return new CNAMEData(cname);
    }

    @ConstructorProperties("cname")
    CNAMEData(String cname) {
        put("cname", checkNotNull(cname, "cname"));
    }

    /**
     * domain-name which specifies the canonical or primary name for the owner.
     * The owner name is an alias.
     * 
     * @since 1.3
     */
    public String cname() {
        return get("cname").toString();
    }

    private static final long serialVersionUID = 1L;
}
