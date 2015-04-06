package denominator.model;

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

  private final String id;
  private final String name;
  private final int ttl;
  private final String email;

  Zone(String id, String name, int ttl, String email) {
    this.id = id;
    this.name = checkNotNull(name, "name");
    this.email = checkNotNull(email, "email of %s", name);
    checkArgument(ttl >= 0, "Invalid ttl value: %s, must be 0-%s", ttl, Integer.MAX_VALUE);
    this.ttl = ttl;
  }

  /**
   * The potentially transient and opaque string that uniquely identifies the zone. This may be null
   * when used as an input object.
   *
   * @since 4.5
   */
  public String id() {
    return id;
  }

  /**
   * The origin or starting point for the zone in the DNS tree. Usually includes a trailing dot, ex.
   * "{@code netflix.com.}"
   *
   * <p/> The name of a zone cannot be changed.
   */
  public String name() {
    return name;
  }

  /**
   * The {@link ResourceRecordSet#ttl() ttl} of the zone's {@link denominator.model.rdata.SOAData
   * SOA} record.
   *
   * <p/>Caution: Eventhough some providers use this as a default ttl for new records, this is not
   * always the case.
   *
   * @since 4.5
   */
  public int ttl() {
    return ttl;
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
      return equal(id(), other.id())
             && name().equals(other.name())
             && ttl() == other.ttl()
             && email().equals(other.email());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (id() != null ? id().hashCode() : 0);
    result = 31 * result + name().hashCode();
    result = 31 * result + ttl();
    result = 31 * result + email().hashCode();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Zone [");
    if (!name().equals(id())) {
      builder.append("id=").append(id()).append(", ");
    }
    builder.append("name=").append(name());
    builder.append(", ").append("ttl=").append(ttl());
    builder.append(", ").append("email=").append(email());
    builder.append("]");
    return builder.toString();
  }

  /**
   * Represent a zone when its {@link #id() id} is its name.
   *
   * @param name corresponds to {@link #name()} and {@link #id()}
   * @deprecated Use {@link #create(String, String, int, String)}. This will be removed in version
   * 5.
   */
  @Deprecated
  public static Zone create(String name) {
    return create(name, name);
  }

  /**
   * Represent a zone with a fake email and a TTL of 86400.
   *
   * @param name corresponds to {@link #name()}
   * @param id   nullable, corresponds to {@link #id()}
   * @deprecated Use {@link #create(String, String, int, String)}. This will be removed in version
   * 5.
   */
  @Deprecated
  public static Zone create(String name, String id) {
    return new Zone(id, name, 86400, "nil@" + name);
  }

  /**
   * @param id    nullable, corresponds to {@link #id()}
   * @param name  corresponds to {@link #name()}
   * @param ttl   corresponds to {@link #ttl()}
   * @param email corresponds to {@link #email()}
   */
  public static Zone create(String id, String name, int ttl, String email) {
    return new Zone(id, name, ttl, email);
  }
}
