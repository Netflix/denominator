package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.net.Inet6Address;
import java.net.InetAddress;
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

    public static AAAAData create(String ipv6address) {
        InetAddress address = InetAddresses.forString(checkNotNull(ipv6address, "ipv6address"));
        checkArgument(address instanceof Inet6Address, "%s should be a ipv6 address for AAAA record", address);
        return new AAAAData(Inet6Address.class.cast(address));
    }

    public static AAAAData create(Inet6Address address) {
        return new AAAAData(address);
    }

    @ConstructorProperties("address")
    private AAAAData(Inet6Address address) {
        this.delegate = ImmutableMap.<String, Object> of("address", checkNotNull(address, "address"));
    }

    /**
     * a 128 bit IPv6 address
     */
    public Inet6Address getAddress() {
        return Inet6Address.class.cast(get("address"));
    }

    private final ImmutableMap<String, Object> delegate;

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
