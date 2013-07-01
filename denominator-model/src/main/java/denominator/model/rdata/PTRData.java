package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code PTR} (Pointer) RData
 * 
 * <br><br><b>Example</b><br>
 * 
 * <pre>
 * PTRData rdata = PTRData.create("ptr.foo.com.");
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class PTRData extends ForwardingMap<String, Object> {

    public static PTRData create(String ptrdname) {
        return new PTRData(ptrdname);
    }

    private final String ptrdname;

    @ConstructorProperties("ptrdname")
    private PTRData(String ptrdname) {
        this.ptrdname = checkNotNull(ptrdname, "ptrdname");
        this.delegate = ImmutableMap.<String, Object> of("ptrdname", ptrdname);
    }

    /**
     * domain-name which points to some location in the domain name space.
     * 
     * @since 1.3
     */
    public String ptrdname() {
        return ptrdname;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
