package denominator.model;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import denominator.model.profile.Geo;

/**
 * A grouping of resource records by name and type. In implementation, this is
 * an immutable list of rdata values corresponding to records sharing the same
 * {@link #name() name} and {@link #type}.
 * 
 * @param <D>
 *            RData type shared across elements. This may be empty in the case
 *            of special profile such as `alias`.
 * 
 *            See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class ResourceRecordSet<D extends Map<String, Object>> {

    private final String name;
    private final String type;
    private final String qualifier;
    private final Integer ttl;
    private final List<D> records;
    private final List<Map<String, Object>> profiles;

    @ConstructorProperties({ "name", "type", "qualifier", "ttl", "records", "profiles" })
    ResourceRecordSet(String name, String type, String qualifier, Integer ttl, List<D> records,
            List<Map<String, Object>> profiles) {
        this.name = checkNotNull(name, "name");
        checkArgument(name.length() <= 255, "Name must be limited to 255 characters");
        this.type = checkNotNull(type, "type of %s", name);
        this.qualifier = qualifier;
        this.ttl = ttl;
        if (ttl != null) {
            boolean rfc2181 = ttl >= 0 && ttl.longValue() <= 0x7FFFFFFFL;
            checkArgument(rfc2181, "Invalid ttl value: %s, must be 0-2147483647", this.ttl);
        }
        this.records = checkNotNull(records, "records  of %s %s", name, type);
        this.profiles = checkNotNull(profiles, "profiles of %s %s", name, type);
    }

    /**
     * an owner name, i.e., the name of the node to which this resource record
     * pertains.
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
     * A user-defined identifier that differentiates among multiple resource
     * record sets that have the same combination of DNS name and type. Only
     * present when there's a {@link #profiles() profile} such as {@link Geo
     * geo}, which is otherwise ambiguous on name, type.
     * 
     * @return qualifier or null.
     * @since 1.3
     */
    public String qualifier() {
        return qualifier;
    }

    /**
     * the time interval that the resource record may be cached. Zero implies it
     * is not cached. Absent means default for the zone.
     * 
     * @return ttl or null.
     * @since 1.3
     */
    public Integer ttl() {
        return ttl;
    }

    /**
     * server-side profiles of the record set, often controls visibility based
     * on client origin, latency or server health. If empty, this is a basic
     * record, visible to all resolvers.
     * 
     * For example, if this record set is intended for resolvers in Utah,
     * profiles will include a Map whose entries include {@code type -> "geo"},
     * and is an instance of {@link denominator.model.profile.Geo}, where
     * {@link denominator.model.profile.Geo#regions()} contains something like
     * `Utah` or `US-UT`.
     * 
     * @since 1.3
     */
    public List<Map<String, Object>> profiles() {
        return profiles;
    }

    /**
     * RData type shared across elements. This may be empty in the case of
     * special profile such as `alias`.
     * 
     * @since 2.3
     */
    public List<D> records() {
        return records;
    }

    @Override
    public int hashCode() {
        return 37 * Arrays.hashCode(new Object[] { name, type, qualifier, records, profiles });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ResourceRecordSet))
            return false;
        ResourceRecordSet<?> that = ResourceRecordSet.class.cast(obj);
        if (qualifier == null) {
            if (that.qualifier != null)
                return false;
        } else if (!qualifier.equals(that.qualifier))
            return false;
        return this.name.equals(that.name) && this.type.equals(that.type) && this.records.equals(that.records)
                && this.profiles.equals(that.profiles);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append('{');
        builder.append("name=").append(name).append(',');
        builder.append("type=").append(type);
        if (qualifier != null)
            builder.append(',').append("qualifier=").append(qualifier);
        if (ttl != null)
            builder.append(',').append("ttl=").append(ttl);
        if (!records.isEmpty())
            builder.append(',').append("records=").append(records);
        if (!profiles.isEmpty())
            builder.append(',').append("profiles=").append(profiles);
        return builder.append('}').toString();
    }

    public static <D extends Map<String, Object>> Builder<D> builder() {
        return new Builder<D>();
    }

    /**
     * Allows creation or mutation of record sets based on the portable RData
     * form {@code D} as extends {@code Map<String, Object>}
     * 
     * @param <D>
     *            RData type shared across elements. see package
     *            {@code denominator.model.rdata}
     * 
     */
    public static class Builder<D extends Map<String, Object>> extends AbstractRecordSetBuilder<D, D, Builder<D>> {

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
