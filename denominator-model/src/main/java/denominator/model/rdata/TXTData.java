package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code TXT} (Text) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * TXTData rdata = TXTData.create("=spf1 ip4:192.0.2.1/24 ip4:198.51.100.1/24 -all");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class TXTData extends ForwardingMap<String, Object> {

    public static TXTData create(String txtdata) {
        return new TXTData(txtdata);
    }

    private final String txtdata;

    @ConstructorProperties("txtdata")
    private TXTData(String txtdata) {
        checkArgument(checkNotNull(txtdata, "txtdata").length() <= 65535 , "txt data is limited to 65535");
        this.txtdata = txtdata;
        this.delegate = ImmutableMap.<String, Object> of("txtdata", txtdata);
    }

    /**
     * One or more character-strings.
     */
    public String getTxtdata() {
        return txtdata;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
