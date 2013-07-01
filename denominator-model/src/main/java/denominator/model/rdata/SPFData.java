package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code SPF} (Sender Policy Framework) RData
 * 
 * <br><br><b>Example</b><br>
 * 
 * <pre>
 * import static denominator.model.rdata.SPFData.spf;
 * ...
 * SPFData rdata = spf("v=spf1 +mx a:colo.example.com/28 -all");
 * </pre>
 * 
 * See <a href="http://tools.ietf.org/html/rfc4408#section-3.1.1">RFC 4408</a>
 */
public class SPFData extends ForwardingMap<String, Object> {

    public static SPFData create(String txtdata) {
        return new SPFData(txtdata);
    }

    private final String txtdata;

    @ConstructorProperties("txtdata")
    private SPFData(String txtdata) {
        this.txtdata = checkNotNull(txtdata, "txtdata");
        this.delegate = ImmutableMap.<String, Object> of("txtdata", txtdata);
    }

    /**
     * One or more character-strings.
     * 
     * @since 1.3
     */
    public String txtdata() {
        return txtdata;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
