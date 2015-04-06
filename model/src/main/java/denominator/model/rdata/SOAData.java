package denominator.model.rdata;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code SOA} (Start of Authority) RData
 *
 * <br> <br> <b>Example</b><br>
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
public final class SOAData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  SOAData(String mname, String rname, int serial, int refresh, int retry, int expire, int minimum) {
    checkNotNull(mname, "mname");
    checkNotNull(rname, "rname of %s", mname);
    checkArgument(serial >= 0, "serial of %s must be unsigned", mname);
    checkArgument(refresh >= 0, "refresh of %s must be unsigned", mname);
    checkArgument(retry >= 0, "retry of %s must be unsigned", mname);
    checkArgument(expire >= 0, "expire of %s must be unsigned", mname);
    checkArgument(minimum >= 0, "minimum of %s must be unsigned", mname);
    put("mname", mname);
    put("rname", rname);
    put("serial", serial);
    put("refresh", refresh);
    put("retry", retry);
    put("expire", expire);
    put("minimum", minimum);
  }

  public static SOAData.Builder builder() {
    return new Builder();
  }

  /**
   * domain-name of the name server that was the original or primary source of data for this zone
   *
   * @since 1.3
   */
  public String mname() {
    return get("mname").toString();
  }

  /**
   * domain-name which specifies the mailbox of the person responsible for this zone.
   *
   * @since 1.3
   */
  public String rname() {
    return get("rname").toString();
  }

  /**
   * version number of the original copy of the zone.
   *
   * @since 1.3
   */
  public int serial() {
    return Integer.class.cast(get("serial"));
  }

  /**
   * time interval before the zone should be refreshed
   *
   * @since 1.3
   */
  public int refresh() {
    return Integer.class.cast(get("refresh"));
  }

  /**
   * time interval that should elapse before a failed refresh should be retried
   *
   * @since 1.3
   */
  public int retry() {
    return Integer.class.cast(get("retry"));
  }

  /**
   * time value that specifies the upper limit on the time interval that can elapse before the zone
   * is no longer authoritative.
   *
   * @since 1.3
   */
  public int expire() {
    return Integer.class.cast(get("expire"));
  }

  /**
   * minimum TTL field that should be exported with any RR from this zone.
   *
   * @since 1.3
   */
  public int minimum() {
    return Integer.class.cast(get("minimum"));
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
          .retry(in.retry()).expire(in.expire()).minimum(in.minimum());
    }
  }
}
