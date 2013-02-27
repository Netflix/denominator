package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code SPF} (Sender Policy Framework) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * import static denominator.model.rdata.SPFData.spf;
 * ...
 * SPFData rdata = spf("v=spf1 +mx a:colo.example.com/28 -all");
 * </pre>
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4408#section-3.1.1">RFC 4408</a>
 */
public class SPFData extends ForwardingMap<String, Object> {

    public static SPFData create(String spfdata) {
        return new SPFData(spfdata);
    }

    @ConstructorProperties("spfdata")
    private SPFData(String spfdata) {
        this.delegate = ImmutableMap.<String, Object> of("spfdata", checkNotNull(spfdata, "spfdata"));
    }

    /**
     * One or more character-strings.
     */
    public String getSpfdata() {
        return get("spfdata").toString();
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
