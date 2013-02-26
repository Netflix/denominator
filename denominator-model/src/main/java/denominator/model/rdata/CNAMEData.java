package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code CNAME} (Canonical
 * Name) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * import static denominator.model.rdata.CNAMEData.cname;
 * ...
 * CNAMEData rdata = cname("cname.foo.com.");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class CNAMEData extends ForwardingMap<String, Object> {
    private final ImmutableMap<String, Object> delegate;

    @ConstructorProperties("cname")
    private CNAMEData(String cname) {
        this.delegate = ImmutableMap.<String, Object> of("cname", checkNotNull(cname, "cname"));
    }

    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * domain-name which specifies the canonical or primary name for the owner.
     * The owner name is an alias.
     */
    public String getCname() {
        return get("cname").toString();
    }

    public static CNAMEData cname(String cname) {
        return builder().cname(cname).build();
    }

    public static CNAMEData.Builder builder() {
        return new Builder();
    }

    public CNAMEData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String cname;

        /**
         * @see CNAMEData#getCname()
         */
        public CNAMEData.Builder cname(String cname) {
            this.cname = cname;
            return this;
        }

        public CNAMEData build() {
            return new CNAMEData(cname);
        }

        public CNAMEData.Builder from(CNAMEData in) {
            return this.cname(in.getCname());
        }
    }
}