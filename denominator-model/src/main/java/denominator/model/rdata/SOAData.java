package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code SOA} (Start of
 * Authority) RData
 * 
 * <br><br><b>Example</b><br>
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
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class SOAData extends ForwardingMap<String, Object> {

    private final String mname;
    private final String rname;
    private final int serial;
    private final int refresh;
    private final int retry;
    private final int expire;
    private final int minimum;

    @ConstructorProperties({ "mname", "rname", "serial", "refresh", "retry", "expire", "minimum" })
    private SOAData(String mname, String rname, int serial, int refresh, int retry,
            int expire, int minimum) {
        this.mname = checkNotNull(mname, "mname");
        this.rname = checkNotNull(rname, "rname of %s", mname);
        checkArgument(serial >= 0, "serial of %s must be unsigned", mname);
        this.serial = serial;
        checkArgument(refresh >= 0, "refresh of %s must be unsigned", mname);
        this.refresh = refresh;
        checkArgument(retry >= 0, "retry of %s must be unsigned", mname);
        this.retry = retry;
        checkArgument(expire >= 0, "expire of %s must be unsigned", mname);
        this.expire = expire;
        checkArgument(minimum >= 0, "minimum of %s must be unsigned", mname);
        this.minimum = minimum;
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("mname", mname)
                                    .put("rname", rname)
                                    .put("serial", serial)
                                    .put("refresh", refresh)
                                    .put("retry", retry)
                                    .put("expire", expire)
                                    .put("minimum", minimum).build();
    }

    /**
     * domain-name of the name server that was the original or primary source of
     * data for this zone
     * 
     * @since 1.3
     */
    public String mname() {
        return mname;
    }

    /**
     * domain-name which specifies the mailbox of the person responsible for
     * this zone.
     * 
     * @since 1.3
     */
    public String rname() {
        return rname;
    }

    /**
     * version number of the original copy of the zone.
     * 
     * @since 1.3
     */
    public int serial() {
        return serial;
    }

    /**
     * time interval before the zone should be refreshed
     * 
     * @since 1.3
     */
    public int refresh() {
        return refresh;
    }

    /**
     * time interval that should elapse before a failed refresh should be
     * retried
     * 
     * @since 1.3
     */
    public int retry() {
        return retry;
    }

    /**
     * time value that specifies the upper limit on the time interval that can
     * elapse before the zone is no longer authoritative.
     * 
     * @since 1.3
     */
    public int expire() {
        return expire;
    }

    /**
     * minimum TTL field that should be exported with any RR from this zone.
     * 
     * @since 1.3
     */
    public int minimum() {
        return minimum;
    }

    public SOAData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private String mname;
        private String rname;
        private int serial = -1;
        private int refresh = -1;
        private int retry = -1;
        private int expire = -1;
        private int minimum = -1;

        /**
         * @see SOAData#mname()
         */
        public SOAData.Builder mname(String mname) {
            this.mname = mname;
            return this;
        }

        /**
         * @see SOAData#rname()
         */
        public SOAData.Builder rname(String rname) {
            this.rname = rname;
            return this;
        }

        /**
         * @see SOAData#serial()
         */
        public SOAData.Builder serial(int serial) {
            this.serial = serial;
            return this;
        }

        /**
         * @see SOAData#refresh()
         */
        public SOAData.Builder refresh(int refresh) {
            this.refresh = refresh;
            return this;
        }

        /**
         * @see SOAData#retry()
         */
        public SOAData.Builder retry(int retry) {
            this.retry = retry;
            return this;
        }

        /**
         * @see SOAData#expire()
         */
        public SOAData.Builder expire(int expire) {
            this.expire = expire;
            return this;
        }

        /**
         * @see SOAData#minimum()
         */
        public SOAData.Builder minimum(int minimum) {
            this.minimum = minimum;
            return this;
        }

        public SOAData build() {
            return new SOAData(mname, rname, serial, refresh, retry, expire, minimum);
        }

        public SOAData.Builder from(SOAData in) {
            return this.mname(in.mname()).rname(in.rname()).serial(in.serial()).refresh(in.refresh())
                    .expire(in.expire()).minimum(in.minimum());
        }
    }

    public static SOAData.Builder builder() {
        return new Builder();
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
