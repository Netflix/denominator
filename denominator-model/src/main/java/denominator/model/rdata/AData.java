package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.net.Inet4Address;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;

/**
 * Corresponds to the binary representation of the {@code A} (Address) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * AData rdata = AData.create(&quot;192.0.2.1&quot;);
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class AData extends ForwardingMap<String, Object> {

    /**
     * 
     * @param ipv4address
     *            valid ipv4 address. ex. {@code 192.0.2.1}
     * @throws IllegalArgumentException
     *             if the address is malformed or not ipv4
     * @see InetAddresses#forString(String)
     */
    public static AData create(String ipv4address) throws IllegalArgumentException {
        return new AData(ipv4address);
    }

    private final String address;

    @ConstructorProperties("address")
    private AData(String ipv4address) {
        checkArgument(InetAddresses.forString(checkNotNull(ipv4address, "address")) instanceof Inet4Address,
                "%s should be a ipv4 address", ipv4address);
        this.address = ipv4address;
        this.delegate = ImmutableMap.<String, Object> of("address", ipv4address);
    }

    /**
     * a 32-bit internet address
     */
    public String getAddress() {
        return address;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
