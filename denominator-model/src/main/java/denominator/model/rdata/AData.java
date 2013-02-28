package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.net.Inet4Address;
import java.net.InetAddress;
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
 * AData rdata = AData.create("1.1.1.1");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class AData extends ForwardingMap<String, Object> {

    public static AData create(String ipv4address) {
        InetAddress address = InetAddresses.forString(checkNotNull(ipv4address, "ipv4address"));
        checkArgument(address instanceof Inet4Address, "%s should be a ipv4 address for A record", address);
        return new AData(Inet4Address.class.cast(address));
    }

    public static AData create(Inet4Address address) {
        return new AData(address);
    }

    @ConstructorProperties("address")
    private AData(Inet4Address address) {
        this.delegate = ImmutableMap.<String, Object> of("address", checkNotNull(address, "address"));
    }

    /**
     * a 32-bit internet address
     */
    public Inet4Address getAddress() {
        return Inet4Address.class.cast(get("address"));
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
