package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;

/**
 * Corresponds to the binary representation of the {@code SOA} (Start of
 * Authority) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * SOAData rdata = SOAData.builder()
 *                        .rname(&quot;foo.com.&quot;)
 *                        .mname(&quot;admin.foo.com.&quot;)
 *                        .serial(1)
 *                        .refresh(3600)
 *                        .retry(600)
 *                        .expire(604800)
 *                        .minimum(60).build()
 * </pre>
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class SOAData extends ForwardingMap<String, Object> {

    @ConstructorProperties({ "mname", "rname", "serial", "refresh", "retry", "expire", "minimum" })
    private SOAData(String mname, String rname, UnsignedInteger serial, UnsignedInteger refresh, UnsignedInteger retry,
            UnsignedInteger expire, UnsignedInteger minimum) {
        this.delegate = ImmutableMap.<String, Object> builder()
                .put("mname", checkNotNull(mname, "mname"))
                .put("rname", checkNotNull(rname, "rname of %s", mname))
                .put("serial", checkNotNull(serial, "serial of %s", mname))
                .put("refresh", checkNotNull(refresh, "refresh of %s", mname))
                .put("retry", checkNotNull(retry, "retry of %s", mname))
                .put("expire", checkNotNull(expire, "expire of %s", mname))
                .put("minimum", checkNotNull(minimum, "minimum of %s", mname)).build();
    }

    /**
     * domain-name of the name server that was the original or primary source of
     * data for this zone
     */
    public String getMname() {
        return String.class.cast(get("mname"));
    }

    /**
     * domain-name which specifies the mailbox of the person responsible for
     * this zone.
     */
    public String getRname() {
        return String.class.cast(get("rname"));
    }

    /**
     * version number of the original copy of the zone.
     */
    public UnsignedInteger getSerial() {
        return UnsignedInteger.class.cast(get("serial"));
    }

    /**
     * time interval before the zone should be refreshed
     */
    public UnsignedInteger getRefresh() {
        return UnsignedInteger.class.cast(get("refresh"));
    }

    /**
     * time interval that should elapse before a failed refresh should be
     * retried
     */
    public UnsignedInteger getRetry() {
        return UnsignedInteger.class.cast(get("retry"));
    }

    /**
     * time value that specifies the upper limit on the time interval that can
     * elapse before the zone is no longer authoritative.
     */
    public UnsignedInteger getExpire() {
        return UnsignedInteger.class.cast(get("expire"));
    }

    /**
     * minimum TTL field that should be exported with any RR from this zone.
     */
    public UnsignedInteger getMinimum() {
        return UnsignedInteger.class.cast(get("minimum"));
    }

    public SOAData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String mname;
        private String rname;
        private UnsignedInteger serial;
        private UnsignedInteger refresh;
        private UnsignedInteger retry;
        private UnsignedInteger expire;
        private UnsignedInteger minimum;

        /**
         * @see SOAData#getMname()
         */
        public SOAData.Builder mname(String mname) {
            this.mname = mname;
            return this;
        }

        /**
         * @see SOAData#getRname()
         */
        public SOAData.Builder rname(String rname) {
            this.rname = rname;
            return this;
        }

        /**
         * @see SOAData#getSerial()
         */
        public SOAData.Builder serial(UnsignedInteger serial) {
            this.serial = serial;
            return this;
        }

        /**
         * @see SOAData#getSerial()
         */
        public SOAData.Builder serial(int serial) {
            return serial(UnsignedInteger.fromIntBits(serial));
        }

        /**
         * @see SOAData#getRefresh()
         */
        public SOAData.Builder refresh(UnsignedInteger refresh) {
            this.refresh = refresh;
            return this;
        }

        /**
         * @see SOAData#getRefresh()
         */
        public SOAData.Builder refresh(int refresh) {
            return refresh(UnsignedInteger.fromIntBits(refresh));
        }

        /**
         * @see SOAData#getRetry()
         */
        public SOAData.Builder retry(UnsignedInteger retry) {
            this.retry = retry;
            return this;
        }

        /**
         * @see SOAData#getRetry()
         */
        public SOAData.Builder retry(int retry) {
            return retry(UnsignedInteger.fromIntBits(retry));
        }

        /**
         * @see SOAData#getExpire()
         */
        public SOAData.Builder expire(UnsignedInteger expire) {
            this.expire = expire;
            return this;
        }

        /**
         * @see SOAData#getExpire()
         */
        public SOAData.Builder expire(int expire) {
            return expire(UnsignedInteger.fromIntBits(expire));
        }

        /**
         * @see SOAData#getMinimum()
         */
        public SOAData.Builder minimum(UnsignedInteger minimum) {
            this.minimum = minimum;
            return this;
        }

        /**
         * @see SOAData#getMinimum()
         */
        public SOAData.Builder minimum(int minimum) {
            return minimum(UnsignedInteger.fromIntBits(minimum));
        }

        public SOAData build() {
            return new SOAData(mname, rname, serial, refresh, retry, expire, minimum);
        }

        public SOAData.Builder from(SOAData in) {
            return this.mname(in.getMname()).rname(in.getRname()).serial(in.getSerial()).refresh(in.getRefresh())
                    .expire(in.getExpire()).minimum(in.getMinimum());
        }
    }

    public static SOAData.Builder builder() {
        return new Builder();
    }

    private final ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
