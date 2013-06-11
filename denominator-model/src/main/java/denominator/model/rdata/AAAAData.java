package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.net.Inet6Address;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;

/**
 * Corresponds to the binary representation of the {@code AAAA} (Address) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * AAAAData rdata = AAAAData.create(&quot;1234:ab00:ff00::6b14:abcd&quot;);
 * </pre>
 * 
 * @see <aaaa href="http://www.ietf.org/rfc/rfc3596.txt">RFC 3596</aaaa>
 */
public class AAAAData extends ForwardingMap<String, Object> {

    /**
     * 
     * @param ipv6address
     *            valid ipv6 address. ex. {@code 1234:ab00:ff00::6b14:abcd}
     * @throws IllegalArgumentException
     *             if the address is malformed or not ipv6
     * @see InetAddresses#forString(String)
     */
    public static AAAAData create(String ipv6address) throws IllegalArgumentException {
        return new AAAAData(ipv6address);
    }

    private final String address;

    @ConstructorProperties("address")
    private AAAAData(String ipv6address) {
        checkArgument(InetAddresses.forString(checkNotNull(ipv6address, "address")) instanceof Inet6Address,
                "%s should be a ipv6 address", ipv6address);
        this.address = ipv6address;
        this.delegate = ImmutableMap.<String, Object> of("address", ipv6address);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #address()}
     */
    @Deprecated
    public String getAddress() {
        return address();
    }

    /**
     * a 128 bit IPv6 address
     * 
     * @since 1.3
     */
    public String address() {
        return address;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
