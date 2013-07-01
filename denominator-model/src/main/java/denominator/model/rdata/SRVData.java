package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

/**
 * Corresponds to the binary representation of the {@code SRV} (Service) RData
 * 
 * <br><br><b>Example</b><br>
 * 
 * <pre>
 * SRVData rdata = SRVData.builder()
 *                        .priority(0)
 *                        .weight(1)
 *                        .port(80)
 *                        .target(&quot;www.foo.com.&quot;).build()
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc2782.txt">RFC 2782</a>
 */
public class SRVData extends ForwardingMap<String, Object> {

    private final int priority;
    private final int weight;
    private final int port;
    private final String target;

    @ConstructorProperties({ "priority", "weight", "port", "target" })
    private SRVData(int priority, int weight, int port, String target) {
        checkArgument(priority <= 0xFFFF, "priority must be 0-65535");
        checkArgument(weight <= 0xFFFF, "weight must be 0-65535");
        checkArgument(port <= 0xFFFF, "port must be 0-65535");
        this.priority = priority;
        this.weight = weight;
        this.port = port;
        this.target = checkNotNull(target, "target");
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("priority", priority)
                                    .put("weight", weight)
                                    .put("port", port)
                                    .put("target", target).build();
    }

    /**
     * The priority of this target host. A client MUST attempt to contact the
     * target host with the lowest-numbered priority it can reach; target hosts
     * with the same priority SHOULD be tried in an order defined by the weight
     * field.
     * 
     * @since 1.3
     */
    public int priority() {
        return priority;
    }

    /**
     * The weight field specifies a relative weight for entries with the same
     * priority. Larger weights SHOULD be given a proportionately higher
     * probability of being selected.
     * 
     * @since 1.3
     */
    public int weight() {
        return weight;
    }

    /**
     * The port on this target host of this service.
     * 
     * @since 1.3
     */
    public int port() {
        return port;
    }

    /**
     * The domain name of the target host. There MUST be one or more address
     * records for this name, the name MUST NOT be an alias.
     * 
     * @since 1.3
     */
    public String target() {
        return target;
    }

    public SRVData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private int priority = -1;
        private int weight = -1;
        private int port = -1;
        private String target;

        /**
         * @see SRVData#priority()
         */
        public SRVData.Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @see SRVData#weight()
         */
        public SRVData.Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /**
         * @see SRVData#port()
         */
        public SRVData.Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * @see SRVData#target()
         */
        public SRVData.Builder target(String target) {
            this.target = target;
            return this;
        }

        public SRVData build() {
            return new SRVData(priority, weight, port, target);
        }

        public SRVData.Builder from(SRVData in) {
            return this.priority(in.priority()).weight(in.weight()).port(in.port()).target(in.target());
        }
    }

    public static SRVData.Builder builder() {
        return new Builder();
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
