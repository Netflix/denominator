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
 * TXTData rdata = TXTData.create("=spf1 ip4:1.1.1.1/24 ip4:2.2.2.2/24 -all");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class TXTData extends ForwardingMap<String, Object> {

    public static TXTData create(String txtdata) {
        return new TXTData(txtdata);
    }

    @ConstructorProperties("txtdata")
    private TXTData(String txtdata) {
        checkArgument(checkNotNull(txtdata, "txtdata").length() <= 65535 , "txt data is limited to 65535");
        this.delegate = ImmutableMap.<String, Object> of("txtdata", txtdata);
    }

    /**
     * One or more character-strings.
     */
    public String getTxtdata() {
        return get("txtdata").toString();
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}