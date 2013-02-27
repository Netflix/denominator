package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code NS} (Name Server)
 * RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * import static denominator.model.rdata.NSData.ns;
 * ...
 * NSData rdata = ns("ns.foo.com.");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class NSData extends ForwardingMap<String, Object> {
    private final ImmutableMap<String, Object> delegate;

    @ConstructorProperties("nsdname")
    private NSData(String nsdname) {
        this.delegate = ImmutableMap.<String, Object> of("nsdname", checkNotNull(nsdname, "nsdname"));
    }

    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * domain-name which specifies a host which should be authoritative for the
     * specified class and domain.
     */
    public String getNsdname() {
        return get("nsdname").toString();
    }

    public static NSData ns(String nsdname) {
        return builder().nsdname(nsdname).build();
    }

    public static NSData.Builder builder() {
        return new Builder();
    }

    public NSData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String nsdname;

        /**
         * @see NSData#getNsdname()
         */
        public NSData.Builder nsdname(String nsdname) {
            this.nsdname = nsdname;
            return this;
        }

        public NSData build() {
            return new NSData(nsdname);
        }

        public NSData.Builder from(NSData in) {
            return this.nsdname(in.getNsdname());
        }
    }
}