package denominator.model.rdata;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code SRV} (Service) RData
 *
 * <br> <br> <b>Example</b><br>
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
public final class SRVData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  SRVData(int priority, int weight, int port, String target) {
    checkArgument(priority <= 0xFFFF, "priority must be 0-65535");
    checkArgument(weight <= 0xFFFF, "weight must be 0-65535");
    checkArgument(port <= 0xFFFF, "port must be 0-65535");
    checkNotNull(target, "target");
    put("priority", priority);
    put("weight", weight);
    put("port", port);
    put("target", target);
  }

  public static SRVData.Builder builder() {
    return new Builder();
  }

  /**
   * The priority of this target host. A client MUST attempt to contact the target host with the
   * lowest-numbered priority it can reach; target hosts with the same priority SHOULD be tried in
   * an order defined by the weight field.
   *
   * @since 1.3
   */
  public int priority() {
    return Integer.class.cast(get("priority"));
  }

  /**
   * The weight field specifies a relative weight for entries with the same priority. Larger weights
   * SHOULD be given a proportionately higher probability of being selected.
   *
   * @since 1.3
   */
  public int weight() {
    return Integer.class.cast(get("weight"));
  }

  /**
   * The port on this target host of this service.
   *
   * @since 1.3
   */
  public int port() {
    return Integer.class.cast(get("port"));
  }

  /**
   * The domain name of the target host. There MUST be one or more address records for this name,
   * the name MUST NOT be an alias.
   *
   * @since 1.3
   */
  public String target() {
    return get("target").toString();
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
}
