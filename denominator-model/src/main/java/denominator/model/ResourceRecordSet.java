package denominator.model;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedInteger;

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
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class ResourceRecordSet<D extends Map<String, Object>> implements List<D> {

    private final String name;
    private final String type;
    private final Optional<String> qualifier;
    private final Optional<Integer> ttl;
    private final ImmutableList<D> rdata;
    private final ImmutableList<Map<String, Object>> profiles;

    @ConstructorProperties({ "name", "type", "qualifier", "ttl", "rdata", "profiles" })
    ResourceRecordSet(String name, String type, Optional<String> qualifier, Optional<Integer> ttl, ImmutableList<D> rdata,
            ImmutableList<Map<String, Object>> profiles) {
        this.name = checkNotNull(name, "name");
        checkArgument(name.length() <= 255, "Name must be limited to 255 characters"); 
        this.type = checkNotNull(type, "type of %s", name);
        this.qualifier = checkNotNull(qualifier, "qualifier of %s %s", name, type);
        this.ttl = checkNotNull(ttl, "ttl of %s %s", name, type);
        checkArgument(UnsignedInteger.fromIntBits(this.ttl.or(0)).longValue() <= 0x7FFFFFFFL, // Per RFC 2181 
                "Invalid ttl value: %s, must be 0-2147483647", this.ttl);
        this.rdata = checkNotNull(rdata, "rdata  of %s %s", name, type);
        this.profiles = checkNotNull(profiles, "profiles of %s %s", name, type);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #name()}
     */
    @Deprecated
    public String getName() {
        return name();
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
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #type()}
     */
    @Deprecated
    public String getType() {
        return type();
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
     * @since 1.3
     */
    public Optional<String> qualifier() {
        return qualifier;
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #ttl()}
     */
    @Deprecated
    public Optional<Integer> getTTL() {
        return ttl();
    }

    /**
     * the time interval that the resource record may be cached. Zero implies it
     * is not cached. Absent means default for the zone.
     * 
     * @since 1.3
     */
    public Optional<Integer> ttl() {
        return ttl;
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #profiles()}
     */
    @Deprecated
    public ImmutableList<Map<String, Object>> getProfiles() {
        return profiles();
    }

    /**
     * server-side profiles of the record set, often controls visibility based on
     * client origin, latency or server health. If empty, this is a basic record,
     * visible to all resolvers.
     * 
     * For example, if this record set is intended for resolvers in Utah,
     * profiles will include a Map whose entries include {@code type -> "geo"},
     * and is an instance of {@link denominator.model.profile.Geo}, where
     * {@link denominator.model.profile.Geo#regions()} contains something
     * like `Utah` or `US-UT`.
     * 
     * @since 1.3
     */
    public ImmutableList<Map<String, Object>> profiles() {
        return profiles;
    }

    /**
     * RData type shared across elements. This may be empty in the case of
     * special profile such as `alias`.
     * 
     * @since 1.3
     */
    public List<D> rdata() {
        return rdata;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, type, qualifier, rdata, profiles);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ResourceRecordSet))
            return false;
        ResourceRecordSet<?> that = ResourceRecordSet.class.cast(obj);
        return equal(this.name, that.name) && equal(this.type, that.type) && equal(this.qualifier, that.qualifier)
                && equal(this.rdata, that.rdata) && equal(this.profiles, that.profiles);
    }

    @Override
    public String toString() {
        return toStringHelper(this).omitNullValues()
                                   .add("name", name)
                                   .add("type", type)
                                   .add("qualifier", qualifier.orNull())
                                   .add("ttl", ttl.orNull())
                                   .add("rdata", rdata.isEmpty() ? null : rdata)
                                   .add("profiles", profiles.isEmpty() ? null : profiles).toString();
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

        private ImmutableList.Builder<D> rdata = ImmutableList.builder();

        /**
         * adds a value to the builder.
         * 
         * ex.
         * 
         * <pre>
         * builder.add(srvData);
         * </pre>
         */
        public Builder<D> add(D rdata) {
            this.rdata.add(checkNotNull(rdata, "rdata"));
            return this;
        }

        /**
         * replaces all rdata values in the builder
         * 
         * ex.
         * 
         * <pre>
         * builder.addAll(srvData1, srvData2);
         * </pre>
         */
        public Builder<D> addAll(D... rdata) {
            this.rdata.addAll(ImmutableList.copyOf(checkNotNull(rdata, "rdata")));
            return this;
        }

        /**
         * replaces all rdata values in the builder
         * 
         * ex.
         * 
         * <pre>
         * 
         * builder.addAll(otherRecordSet);
         * </pre>
         */
        public <R extends D> Builder<D> addAll(Iterable<R> rdata) {
            this.rdata.addAll(checkNotNull(rdata, "rdata"));
            return this;
        }

        @Override
        protected ImmutableList<D> rdataValues() {
            return rdata.build();
        }
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean add(D e) {
        return rdata.add(e);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public void add(int index, D element) {
        rdata.add(index, element);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean addAll(Collection<? extends D> c) {
        return rdata.addAll(c);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean addAll(int index, Collection<? extends D> c) {
        return rdata.addAll(index, c);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public void clear() {
        rdata.clear();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean contains(Object o) {
        return rdata.contains(o);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean containsAll(Collection<?> c) {
        return rdata.containsAll(c);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public D get(int index) {
        return rdata.get(index);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public int indexOf(Object o) {
        return rdata.indexOf(o);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean isEmpty() {
        return rdata.isEmpty();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public Iterator<D> iterator() {
        return rdata.iterator();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public int lastIndexOf(Object o) {
        return rdata.lastIndexOf(o);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public ListIterator<D> listIterator() {
        return rdata.listIterator();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public ListIterator<D> listIterator(int index) {
        return rdata.listIterator(index);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean remove(Object o) {
        return rdata.remove(o);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public D remove(int index) {
        return rdata.remove(index);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean removeAll(Collection<?> c) {
        return rdata.removeAll(c);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public boolean retainAll(Collection<?> c) {
        return rdata.retainAll(c);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public D set(int index, D element) {
        return rdata.set(index, element);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public int size() {
        return rdata.size();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public List<D> subList(int fromIndex, int toIndex) {
        return rdata.subList(fromIndex, toIndex);
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public Object[] toArray() {
        return rdata.toArray();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #rdata()}
     */
    @Deprecated
    @Override
    public <T> T[] toArray(T[] a) {
        return rdata.toArray(a);
    }
}
