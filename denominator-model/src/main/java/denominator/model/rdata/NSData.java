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
 * NSData rdata = NSData.create("ns.foo.com.");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class NSData extends ForwardingMap<String, Object> {

    public static NSData create(String nsdname) {
        return new NSData(nsdname);
    }

    @ConstructorProperties("nsdname")
    private NSData(String nsdname) {
        this.delegate = ImmutableMap.<String, Object> of("nsdname", checkNotNull(nsdname, "nsdname"));
    }

    /**
     * domain-name which specifies a host which should be authoritative for the
     * specified class and domain.
     */
    public String getNsdname() {
        return get("nsdname").toString();
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}