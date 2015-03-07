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

  Zone(String name, String id) {
    this.name = checkNotNull(name, "name");
    this.id = id;
  }

  /**
   * Represent a zone without an {@link #id() id}.
   *
   * @param name corresponds to {@link #name()}
   */
  public static Zone create(String name) {
    return create(name, null);
  }

  /**
   * Represent a zone with an {@link #id() id}.
   *
   * @param name corresponds to {@link #name()}
   * @param id   nullable; corresponds to {@link #id()}
   */
  public static Zone create(String name, String id) {
    return new Zone(name, id);
  }

  /**
   * The origin or starting point for the zone in the DNS tree. Usually includes a trailing dot, ex.
   * "{@code netflix.com.}"
   */
  public String name() {
    return name;
  }

  /**
   * When present, the service supports multiple zones with the same {@link #name}. When absent, it
   * doesn't. The value is likely to have been system generated. Even if a provider has an id
   * associated with a zone, if it isn't used by their api calls, this method will return null.
   *
   * @see #idOrName()
   */
  public String id() {
    return id;
  }

  /**
   * It is possible that some zones do not have an id, and in this case the name is used. The
   * following form will ensure you get a reference regardless.
   *
   * In implementation, this method is the same as calling: {@code zone.id().or(zone.name())}
   *
   * <br> If {@code denominator.Provider#supportsDuplicateZoneNames()} is true, this will return an
   * id.
   *
   * @return {@link #id() id} or {@link #name() name} if absent
   */
  public String idOrName() {
    return id() != null ? id() : name();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Zone) {
      Zone other = (Zone) obj;
      return equal(name(), other.name())
             && equal(id(), other.id());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name().hashCode();
    result = 31 * result + (id() != null ? id().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Zone [");
    builder.append("name=").append(name());
    if (id() != null) {
      builder.append(", ").append("id=").append(id());
    }
    builder.append("]");
    return builder.toString();
  }
}
