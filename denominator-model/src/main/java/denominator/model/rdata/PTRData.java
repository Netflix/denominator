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
 * PTRData rdata = PTRData.create("ptr.foo.com.");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class PTRData extends ForwardingMap<String, Object> {

    public static PTRData create(String ptrdname) {
        return new PTRData(ptrdname);
    }

    @ConstructorProperties("ptrdname")
    private PTRData(String ptrdname) {
        this.delegate = ImmutableMap.<String, Object> of("ptrdname", checkNotNull(ptrdname, "ptrdname"));
    }

    /**
     * domain-name which points to some location in the domain name space.
     */
    public String getPtrdname() {
        return get("ptrdname").toString();
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}