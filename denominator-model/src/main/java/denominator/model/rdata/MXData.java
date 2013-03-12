package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

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
        return new MXData(preference, exchange);
    }

    private final int preference;
    private final String exchange;

    @ConstructorProperties({ "preference", "exchange" })
    private MXData(int preference, String exchange) {
        checkArgument(preference <= 0xFFFF, "preference must be 65535 or less");
        this.preference = preference;
        this.exchange = checkNotNull(exchange, "exchange");
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("preference", preference)
                                    .put("exchange", exchange).build();
    }

    /**
     * specifies the preference given to this RR among others at the same owner.
     * Lower values are preferred.
     */
    public int getPreference() {
        return preference;
    }

    /**
     * domain-name which specifies a host willing to act as a mail exchange for
     * the owner name.
     */
    public String getExchange() {
        return exchange;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
