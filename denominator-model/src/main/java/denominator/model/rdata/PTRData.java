package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code PTR} (Pointer) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * import static denominator.model.rdata.PTRData.ptr;
 * ...
 * PTRData rdata = ptr("ptr.foo.com.");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class PTRData extends ForwardingMap<String, Object> {
    private final ImmutableMap<String, Object> delegate;

    @ConstructorProperties("ptrdname")
    private PTRData(String ptrdname) {
        this.delegate = ImmutableMap.<String, Object> of("ptrdname", checkNotNull(ptrdname, "ptrdname"));
    }

    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * domain-name which points to some location in the domain name space.
     */
    public String getPtrdname() {
        return get("ptrdname").toString();
    }

    public static PTRData ptr(String ptrdname) {
        return builder().ptrdname(ptrdname).build();
    }

    public static PTRData.Builder builder() {
        return new Builder();
    }

    public PTRData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String ptrdname;

        /**
         * @see PTRData#getPtrdname()
         */
        public PTRData.Builder ptrdname(String ptrdname) {
            this.ptrdname = ptrdname;
            return this;
        }

        public PTRData build() {
            return new PTRData(ptrdname);
        }

        public PTRData.Builder from(PTRData in) {
            return this.ptrdname(in.getPtrdname());
        }
    }
}