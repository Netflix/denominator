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
import denominator.model.profile.Weighted;

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
public class ResourceRecordSet<D extends Map<String, Object>> extends NumbersAreUnsignedIntsLinkedHashMap {

    @SuppressWarnings("unused")
    private ResourceRecordSet() {
    }

    @SuppressWarnings("unchecked")
    @ConstructorProperties({ "name", "type", "qualifier", "ttl", "records", "geo", "weighted", "profiles" })
    ResourceRecordSet(String name, String type, String qualifier, Integer ttl, List<D> records,
            Geo geo, Weighted weighted, List<Map<String, Object>> profiles) {
        checkArgument(checkNotNull(name, "name").length() <= 255, "Name must be limited to 255 characters");
        put("name", name);
        put("type", checkNotNull(type, "type of %s", name));
        if (qualifier != null) {
            put("qualifier", qualifier);
        }
        if (ttl != null) {
            boolean rfc2181 = ttl >= 0 && ttl.longValue() <= 0x7FFFFFFFL;
            checkArgument(rfc2181, "Invalid ttl value: %s, must be 0-2147483647", ttl);
            put("ttl", ttl);
        }
        if (records != null) {
            put("records", records);
        }
        // TODO: remove in 4.0
        List<Map<String, Object>> updateProfiles = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> profile : profiles != null ? profiles : new ArrayList<Map<String, Object>>()) {
            if ("geo".equals(profile.get("type")) && geo == null) {
                geo = Geo.create(Map.class.cast(profile.get("regions")));
            } else if ("weighted".equals(profile.get("type")) && weighted == null) {
                weighted = Weighted.create(Integer.class.cast(profile.get("weight")));
            } else {
                updateProfiles.add(profile);
            }
        }
        if (geo != null) {
            put("geo", geo);
            updateProfiles.add(geo);
        }
        if (weighted != null) {
            put("weighted", weighted);
            updateProfiles.add(weighted);
        }
        // TODO: remove in 4.0
        put("profiles", updateProfiles);
    }

    /**
     * an owner name, i.e., the name of the node to which this resource record
     * pertains.
     * 
     * @since 1.3
     */
    public String name() {
        return get("name").toString();
    }

    /**
     * The mnemonic type of the record. ex {@code CNAME}
     * 
     * @since 1.3
     */
    public String type() {
        return get("type").toString();
    }

    /**
     * A user-defined identifier that differentiates among multiple resource
     * record sets that have the same combination of DNS name and type. Only
     * present when there's a {@link Geo geo} or {@link Weighted} profile which
     * affects visibility to resolvers.
     * 
     * @return qualifier or null.
     * @since 1.3
     */
    public String qualifier() {
        return (String) get("qualifier");
    }

    /**
     * the time interval that the resource record may be cached. Zero implies it
     * is not cached. Absent means default for the zone.
     * 
     * @return ttl or null.
     * @since 1.3
     */
    public Integer ttl() {
        return (Integer) get("ttl");
    }

    /**
     * When present, this record set has a {@link #qualifier() qualifier} and is
     * visible to a subset of all {@link Geo#regions() regions}.
     * 
     * <br>
     * For example, if this record set is intended for resolvers in Utah, the
     * geo profile would be present and its
     * {@link denominator.model.profile.Geo#regions() regions} might contain
     * `Utah` or `US-UT`.
     * 
     * @return geo profile or null.
     * @since 3.7
     */
    public Geo geo() {
        return (Geo) get("geo");
    }

    /**
     * When present, this record set has a {@link #qualifier() qualifier} and is
     * served to its {@link Weighted#weight() weight}.
     * 
     * @return geo profile or null.
     * @since 3.7
     */
    public Weighted weighted() {
        return (Weighted) get("weighted");
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
     * @deprecated will be removed in version 4.0 for type-safe accessors such
     *             as {@link #geo()} and {@link #weighted()}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> profiles() {
        return List.class.cast(get("profiles"));
    }

    /**
     * RData type shared across elements. This may be empty in the case of
     * special profile such as `alias`.
     * 
     * @since 2.3
     */
    @SuppressWarnings("unchecked")
    public List<D> records() {
        return List.class.cast(get("records"));
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

    private static final long serialVersionUID = 1L;
}
