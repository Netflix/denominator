package denominator.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;

/**
 * A grouping of resource records by name and type. In implementation, this is an unmodifiable list
 * of rdata values corresponding to records sharing the same {@link #name() name} and {@link
 * #type}.
 *
 * @param <D> RData type shared across elements. This may be empty in the case of special profile
 *            such as `alias`.
 *
 *            See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class ResourceRecordSet<D extends Map<String, Object>> {

  private final String name;
  private final String type;
  private final String qualifier;
  private final Integer ttl;
  private final List<D> records;
  private final Geo geo;
  private final Weighted weighted;

  ResourceRecordSet(String name, String type, String qualifier, Integer ttl, List<D> records,
                    Geo geo, Weighted weighted) {
    checkArgument(checkNotNull(name, "name").length() <= 255, "Name must be <= 255 characters");
    this.name = name;
    this.type = checkNotNull(type, "type of %s", name);
    this.qualifier = qualifier;
    if (ttl != null) {
      boolean rfc2181 = ttl >= 0 && ttl.longValue() <= 0x7FFFFFFFL;
      checkArgument(rfc2181, "Invalid ttl value: %s, must be 0-2147483647", ttl);
    }
    this.ttl = ttl;
    this.records =
        Collections.unmodifiableList(records != null ? records : Collections.<D>emptyList());
    this.geo = geo;
    this.weighted = weighted;
  }

  public static <D extends Map<String, Object>> Builder<D> builder() {
    return new Builder<D>();
  }

  /**
   * an owner name, i.e., the name of the node to which this resource record pertains.
   *
   * @since 1.3
   */
  public String name() {
    return name;
  }

  /**
   * The mnemonic type of the record. ex {@code CNAME}
   *
   * @since 1.3
   */
  public String type() {
    return type;
  }

  /**
   * A user-defined identifier that differentiates among multiple resource record sets that have the
   * same combination of DNS name and type. Only present when there's a {@link Geo geo} or {@link
   * Weighted} profile which affects visibility to resolvers.
   *
   * @return qualifier or null.
   * @since 1.3
   */
  public String qualifier() {
    return qualifier;
  }

  /**
   * Indicates the time interval that the resource record may be cached. Zero implies it is not
   * cached. Absent means use whatever the default is.
   *
   * <p/>Caution: The concept of a default TTL varies per provider. Some providers use the SOA's
   * ttl, others an account default by record type, and others are hard-coded. It is best to always
   * pass ttl explicitly.
   *
   * @return ttl or null.
   * @since 1.3
   */
  public Integer ttl() {
    return ttl;
  }

  /**
   * When present, this record set has a {@link #qualifier() qualifier} and is visible to a subset
   * of all {@link Geo#regions() regions}.
   *
   * <br> For example, if this record set is intended for resolvers in Utah, the geo profile would
   * be present and its {@link denominator.model.profile.Geo#regions() regions} might contain `Utah`
   * or `US-UT`.
   *
   * @return geo profile or null.
   * @since 3.7
   */
  public Geo geo() {
    return geo;
  }

  /**
   * When present, this record set has a {@link #qualifier() qualifier} and is served to its {@link
   * Weighted#weight() weight}.
   *
   * @return geo profile or null.
   * @since 3.7
   */
  public Weighted weighted() {
    return weighted;
  }

  /**
   * RData type shared across elements. This may be empty in the case of special profile such as
   * `alias`.
   *
   * @since 2.3
   */
  public List<D> records() {
    return records;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ResourceRecordSet) {
      ResourceRecordSet<?> other = (ResourceRecordSet) obj;
      return equal(name(), other.name())
             && equal(type(), other.type())
             && equal(qualifier(), other.qualifier())
             && equal(ttl(), other.ttl())
             && equal(records(), other.records())
             && equal(geo(), other.geo())
             && equal(weighted(), other.weighted());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + name().hashCode();
    result = 31 * result + type().hashCode();
    result = 31 * result + (qualifier() != null ? qualifier().hashCode() : 0);
    result = 31 * result + (ttl() != null ? ttl().hashCode() : 0);
    result = 31 * result + records().hashCode();
    result = 31 * result + (geo() != null ? geo().hashCode() : 0);
    result = 31 * result + (weighted() != null ? weighted().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ResourceRecordSet [");
    builder.append("name=").append(name());
    builder.append(", ").append("type=").append(type());
    if (qualifier() != null) {
      builder.append(", ").append("qualifier=").append(qualifier());
    }
    if (ttl() != null) {
      builder.append(", ").append("ttl=").append(ttl());
    }
    builder.append(", ").append("records=").append(records());
    if (geo() != null) {
      builder.append(", ").append("geo=").append(geo());
    }
    if (weighted() != null) {
      builder.append(", ").append("weighted=").append(weighted());
    }
    builder.append("]");
    return builder.toString();
  }

  /**
   * Allows creation or mutation of record sets based on the portable RData form {@code D} as
   * extends {@code Map<String, Object>}
   *
   * @param <D> RData type shared across elements. see package {@code denominator.model.rdata}
   */
  public static class Builder<D extends Map<String, Object>>
      extends AbstractRecordSetBuilder<D, D, Builder<D>> {

    private List<D> records = new ArrayList<D>();

    /**
     * adds a value to the builder.
     *
     * ex.
     *
     * <pre>
     * builder.add(srvData);
     * </pre>
     */
    public Builder<D> add(D record) {
      this.records.add(checkNotNull(record, "record"));
      return this;
    }

    /**
     * replaces all records values in the builder
     *
     * ex.
     *
     * <pre>
     * builder.addAll(srvData1, srvData2);
     * </pre>
     */
    public Builder<D> addAll(D... records) {
      this.records.addAll(Arrays.asList(checkNotNull(records, "records")));
      return this;
    }

    /**
     * replaces all records values in the builder
     *
     * ex.
     *
     * <pre>
     *
     * builder.addAll(otherRecordSet);
     * </pre>
     */
    public <R extends D> Builder<D> addAll(Collection<R> records) {
      this.records.addAll(checkNotNull(records, "records"));
      return this;
    }

    @Override
    protected List<D> records() {
      return records;
    }
  }
}
