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
 * <br><br><b>Example</b><br>
 * 
 * <pre>
 * NSData rdata = NSData.create("ns.foo.com.");
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class NSData extends ForwardingMap<String, Object> {

    public static NSData create(String nsdname) {
        return new NSData(nsdname);
    }

    private final String nsdname;

    @ConstructorProperties("nsdname")
    private NSData(String nsdname) {
        this.nsdname = checkNotNull(nsdname, "nsdname");
        this.delegate = ImmutableMap.<String, Object> of("nsdname", nsdname);
    }

    /**
     * domain-name which specifies a host which should be authoritative for the
     * specified class and domain.
     * 
     * @since 1.3
     */
    public String nsdname() {
        return nsdname;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
