package denominator.model;

import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;

/**
 * A zone is a delegated portion of DNS. We use the word {@code zone} instead of {@code domain}, as
 * denominator focuses on configuration aspects of DNS.
 *
 * @since 1.2 See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class Zone {

  private final String name;
  private final String id;
  private final String email;

  Zone(String name, String id, String email) {
    this.name = checkNotNull(name, "name");
    this.id = id;
    this.email = checkNotNull(email, "email of %s", name);
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
   * The potentially transient and opaque string that uniquely identifies the zone. This may be null
   * when used as an input object.
   *
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
             && equal(id(), other.id())
             && email().equals(other.email());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name().hashCode();
    result = 31 * result + (id() != null ? id().hashCode() : 0);
    result = 31 * result + email().hashCode();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Zone [");
    builder.append("name=").append(name());
    if (!name().equals(id())) {
      builder.append(", ").append("id=").append(id());
    }
    builder.append(", ").append("email=").append(email());
    builder.append("]");
    return builder.toString();
  }

  /**
   * Represent a zone when its {@link #id() id} is its name.
   *
   * @param name corresponds to {@link #name()} and {@link #id()}
   * @deprecated Use {@link #create(String, String, String)}. This will be removed in version 5.
   */
  @Deprecated
  public static Zone create(String name) {
    return create(name, name);
  }

  /**
   * Represent a zone with a fake email.
   *
   * @param name corresponds to {@link #name()}
   * @param id   nullable, corresponds to {@link #id()}
   * @deprecated Use {@link #create(String, String, String)}. This will be removed in version 5.
   */
  @Deprecated
  public static Zone create(String name, String id) {
    return new Zone(name, id, "fake@" + name);
  }

  /**
   * Represent a zone with a fake email.
   *
   * @param name  corresponds to {@link #name()}
   * @param id    nullable, corresponds to {@link #id()}
   * @param email corresponds to {@link #email()}
   */
  public static Zone create(String name, String id, String email) {
    return new Zone(name, id, email);
  }
}
