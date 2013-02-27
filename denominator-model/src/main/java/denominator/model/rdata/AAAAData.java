package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code AAAA} (Address) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * import static denominator.model.rdata.AAAAData.aaaa;
 * ...
 * AAAAData rdata = aaaa("1234:ab00:ff00::6b14:abcd");
 * </pre>
 * 
 * @see <aaaa href="http://www.ietf.org/rfc/rfc3596.txt">RFC 3596</aaaa>
 */
public class AAAAData extends ForwardingMap<String, Object> {
    private final ImmutableMap<String, Object> delegate;

    @ConstructorProperties("address")
    private AAAAData(String address) {
        this.delegate = ImmutableMap.<String, Object> of("address", checkNotNull(address, "address"));
    }

    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * a 128 bit IPv6 address
     */
    public String getAddress() {
        return get("address").toString();
    }

    public static AAAAData aaaa(String address) {
        return builder().address(address).build();
    }

    public static AAAAData.Builder builder() {
        return new Builder();
    }

    public AAAAData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String address;

        /**
         * @see AAAAData#getAddress()
         */
        public AAAAData.Builder address(String address) {
            this.address = address;
            return this;
        }

        public AAAAData build() {
            return new AAAAData(address);
        }

        public AAAAData.Builder from(AAAAData in) {
            return this.address(in.getAddress());
        }
    }
}