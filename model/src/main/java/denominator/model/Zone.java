package denominator.model;

import denominator.model.rdata.SOAData;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;

/**
 * A zone is a delegated portion of DNS. We use the word {@code zone} instead of {@code domain}, as
 * denominator focuses on configuration aspects of DNS.
 *
 * @since 1.2 See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class Zone {

  /**
   * Metadata about how zones are identified within the scope of a connection to a provider.
   *
   * <p/>For example {@link denominator.model.Zone.Identification#NAME} means you can safely assume
   * a zone's id is its name.
   *
   * <pre>
   * String zoneId;
   * if (mgr.provider().zoneIdentification() == NAME) {
   *    zoneId = zoneName;
   * } else {
   *    zoneId = mgr.api().zones().iterateByName(zoneName).next().id();
   * }
   * </pre>
   *
   * @since 4.5
   */
  public enum Identification {
    /**
     * Only one zone allowed with the same name, and it is {@link Zone#id() identified} by {@link
     * Zone#name() name}.
     */
    NAME,
    /**
     * Only one zone allowed with the same name, but its {@link Zone#id() identifier} is opaque.
     * <p/> Note that a zone with the name name may have different identifiers over time.
     */
    OPAQUE,
    /**
     * Multiple zones are allowed with the same name, and each have a {@link Zone#qualifier()
     * user-defined qualifier}. Each zone's {@link Zone#id() identifier} is opaque. <p/> Note that a
     * zone with the name name may have different identifiers over time.
     */
    QUALIFIED;
  }

  private final String name;
  private final String qualifier;
  private final String id;
  private final String email;
  private final int ttl;

  Zone(String name, String qualifier, String id, String email, int ttl) {
    this.name = checkNotNull(name, "name");
    this.qualifier = qualifier;
    this.id = id;
    this.email = checkNotNull(email, "email of %s", name);
    checkArgument(ttl >= 0, "Invalid ttl value: %s, must be 0-%s", ttl, Integer.MAX_VALUE);
    this.ttl = ttl;
  }

  /**
   * The origin or starting point for the zone in the DNS tree. Usually includes a trailing dot, ex.
   * "{@code netflix.com.}"
   *
   * @see denominator.model.rdata.SOAData#mname()
   */
  public String name() {
    return name;
  }

  /**
   * A user-defined unique string that differentiates zones with the same name. Only supported when
   * the {@code denominator.Provider#zoneIdentification()} is {@link denominator.model.Zone.Identification#QUALIFIED}.
   *
   * @return qualifier or null if the provider doesn't support multiple zones with the same name.
   * @since 4.5
   */
  public String qualifier() {
    return qualifier;
  }

  /**
   * The potentially transient and opaque string that uniquely identifies the zone. This may be null
   * when used as an input object.
   *
   * <p/>Note that this is not used in {@link #hashCode()} or {@link #equals(Object)}, as it may
   * change over time.
   *
   * @return identifier based on {@code denominator.Provider#zoneIdentification()}.
   * @see denominator.model.Zone.Identification
   * @since 4.5
   */
  public String id() {
    return id;
  }

  /**
   * Email contact for the zone. The {@literal @} in the email will be converted to a {@literal .}
   * in the {@link denominator.model.rdata.SOAData#rname() SOA rname field}.
   *
   * @see denominator.model.rdata.SOAData#rname()
   */
  public String email() {
    return email;
  }

  /**
   * Default cache expiration of all resource records that don't declare an {@link
   * denominator.model.Zone#ttl() override}.
   *
   * @see denominator.model.rdata.SOAData#minimum()
   * @since 4.5
   */
  public int ttl() {
    return ttl;
  }

  /**
   * @deprecated only use {@link #id()} when performing operations against a zone. This will be
   * removed in version 5.
   */
  @Deprecated
  public String idOrName() {
    return id() != null ? id() : name();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Zone) {
      Zone other = (Zone) obj;
      return name().equals(other.name())
             && equal(qualifier(), other.qualifier())
             && email().equals(other.email())
             && ttl() == other.ttl();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name().hashCode();
    result = 31 * result + (qualifier() != null ? qualifier().hashCode() : 0);
    result = 31 * result + email().hashCode();
    result = 31 * result + ttl();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Zone [");
    builder.append("name=").append(name());
    if (qualifier() != null) {
      builder.append(", ").append("qualifier=").append(qualifier());
    }
    if (!name().equals(id())) {
      builder.append(", ").append("id=").append(id());
    }
    builder.append(", ").append("email=").append(email());
    builder.append(", ").append("ttl=").append(ttl());
    builder.append("]");
    return builder.toString();
  }

  /**
   * Represent a zone without a {@link #qualifier() qualifier} when its {@link #id() id} is its
   * name.
   *
   * @param name corresponds to {@link #name()} and {@link #id()}
   * @deprecated Use {@link #builder()}. This will be removed in version 5.
   */
  @Deprecated
  public static Zone create(String name) {
    return create(name, name);
  }

  /**
   * Represent a zone without a {@link #qualifier() qualifier}.
   *
   * @param name corresponds to {@link #name()}
   * @param id   corresponds to {@link #id()}
   * @deprecated Use {@link #builder()}. This will be removed in version 5.
   */
  @Deprecated
  public static Zone create(String name, String id) {
    return new Zone(name, null, id, "fake@" + name, 86400);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * @since 4.5
   */
  public static final class Builder {

    private String name;
    private String qualifier;
    private String id;
    private int ttl = 86400;
    private String email;

    /**
     * @see Zone#name()
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * @see Zone#qualifier()
     */
    public Builder qualifier(String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    /**
     * Can be null for input objects.
     *
     * @see Zone#id()
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * @see Zone#email()
     */
    public Builder email(String email) {
      this.email = email;
      return this;
    }

    /**
     * Overrides default of {@code 86400}.
     *
     * @see Zone#ttl()
     */
    public Builder ttl(Integer ttl) {
      this.ttl = ttl;
      return this;
    }

    public Zone build() {
      return new Zone(name, qualifier, id, email, ttl);
    }
  }
}
