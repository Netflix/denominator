package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code A} (Address) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * import static denominator.model.rdata.AData.a;
 * ...
 * AData rdata = a("ptr.foo.com.");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class AData extends ForwardingMap<String, Object> {
    private final ImmutableMap<String, Object> delegate;

    @ConstructorProperties("address")
    private AData(String address) {
        this.delegate = ImmutableMap.<String, Object> of("address", checkNotNull(address, "address"));
    }

    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * a 32-bit internet address
     */
    public String getAddress() {
        return get("address").toString();
    }

    public static AData a(String address) {
        return builder().address(address).build();
    }

    public static AData.Builder builder() {
        return new Builder();
    }

    public AData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String address;

        /**
         * @see AData#getAddress()
         */
        public AData.Builder address(String address) {
            this.address = address;
            return this;
        }

        public AData build() {
            return new AData(address);
        }

        public AData.Builder from(AData in) {
            return this.address(in.getAddress());
        }
    }
}