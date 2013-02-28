package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;

/**
 * Corresponds to the binary representation of the {@code MX} (Mail Exchange)
 * RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * MXData rdata = MXData.create(1, "mail.jclouds.org");
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class MXData extends ForwardingMap<String, Object> {

    public static MXData create(int preference, String exchange) {
        return create(UnsignedInteger.fromIntBits(preference), exchange);
    }

    public static MXData create(UnsignedInteger preference, String exchange) {
        return new MXData(preference, exchange);
    }

    @ConstructorProperties({ "preference", "exchange" })
    private MXData(UnsignedInteger preference, String exchange) {
        this.delegate = ImmutableMap.<String, Object> builder()
                .put("preference", checkNotNull(preference, "preference"))
                .put("exchange", checkNotNull(exchange, "exchange")).build();
    }

    /**
     * specifies the preference given to this RR among others at the same owner.
     * Lower values are preferred.
     */
    public UnsignedInteger getPreference() {
        return UnsignedInteger.class.cast(get("preference"));
    }

    /**
     * domain-name which specifies a host willing to act as a mail exchange for
     * the owner name.
     */
    public String getExchange() {
        return String.class.cast(get("exchange"));
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
