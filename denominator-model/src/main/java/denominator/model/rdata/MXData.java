package denominator.model.rdata;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code MX} (Mail Exchange)
 * RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * MXData rdata = MXData.create(1, &quot;mail.jclouds.org&quot;);
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class MXData extends LinkedHashMap<String, Object> {

    public static MXData create(int preference, String exchange) {
        return new MXData(preference, exchange);
    }

    @ConstructorProperties({ "preference", "exchange" })
    MXData(int preference, String exchange) {
        checkArgument(preference <= 0xFFFF, "preference must be 65535 or less");
        checkNotNull(exchange, "exchange");
        put("preference", preference);
        put("exchange", exchange);
    }

    /**
     * specifies the preference given to this RR among others at the same owner.
     * Lower values are preferred.
     * 
     * @since 1.3
     */
    public int preference() {
        return Integer.class.cast(get("preference"));
    }

    /**
     * domain-name which specifies a host willing to act as a mail exchange for
     * the owner name.
     * 
     * @since 1.3
     */
    public String exchange() {
        return get("exchange").toString();
    }

    private static final long serialVersionUID = 1L;
}
