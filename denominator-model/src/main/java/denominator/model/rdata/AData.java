package denominator.model.rdata;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code A} (Address) RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * AData rdata = AData.create(&quot;192.0.2.1&quot;);
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class AData extends LinkedHashMap<String, Object> {

    /**
     * 
     * @param ipv4address
     *            valid ipv4 address. ex. {@code 192.0.2.1}
     * @throws IllegalArgumentException
     *             if the address is malformed or not ipv4
     */
    public static AData create(String ipv4address) throws IllegalArgumentException {
        return new AData(ipv4address);
    }

    @ConstructorProperties("address")
    AData(String address) {
        checkNotNull(address, "address");
        checkArgument(address.indexOf('.') != -1, "%s should be a ipv4 address", address);
        put("address", address);
    }

    /**
     * a 32-bit internet address
     * 
     * @since 1.3
     */
    public String address() {
        return get("address").toString();
    }

    private static final long serialVersionUID = 1L;
}
