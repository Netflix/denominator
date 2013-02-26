package denominator.model.rdata;

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
 * import static denominator.model.rdata.TXTData.txt;
 * ...
 * TXTData rdata = txt("=spf1 ip4:1.1.1.1/24 ip4:2.2.2.2/24 -all");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class TXTData extends ForwardingMap<String, Object> {
    private final ImmutableMap<String, Object> delegate;

    @ConstructorProperties("txtdata")
    private TXTData(String txtdata) {
        this.delegate = ImmutableMap.<String, Object> of("txtdata", checkNotNull(txtdata, "txtdata"));
    }

    protected Map<String, Object> delegate() {
        return delegate;
    }

    /**
     * One or more character-strings.
     */
    public String getTxtdata() {
        return get("txtdata").toString();
    }

    public static TXTData txt(String txtdata) {
        return builder().txtdata(txtdata).build();
    }

    public static TXTData.Builder builder() {
        return new Builder();
    }

    public TXTData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String txtdata;

        /**
         * @see TXTData#getTxtdata()
         */
        public TXTData.Builder txtdata(String txtdata) {
            this.txtdata = txtdata;
            return this;
        }

        public TXTData build() {
            return new TXTData(txtdata);
        }

        public TXTData.Builder from(TXTData in) {
            return this.txtdata(in.getTxtdata());
        }
    }
}