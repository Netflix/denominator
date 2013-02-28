package denominator.model;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedInteger;

/**
 * A grouping of resource records by name and type. In implementation, this is
 * an immutable list of rdata values corresponding to records sharing the same
 * {@link #getName() name} and {@link #getType}.
 * 
 * @param <D>
 *            RData type shared across elements. see
 *            {@link denominator.model.rdata}
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class ResourceRecordSet<D extends Map<String, Object>> extends ForwardingList<D> {

    final String name;
    final String type;
    final Optional<UnsignedInteger> ttl;
    final ImmutableList<D> rdata;

    @ConstructorProperties({ "name", "type", "ttl", "rdata" })
    ResourceRecordSet(String name, String type, Optional<UnsignedInteger> ttl, ImmutableList<D> rdata) {
        this.name = checkNotNull(name, "name");
        this.type = checkNotNull(type, "type of %s", name);
        this.ttl = checkNotNull(ttl, "ttl of %s", name);
        this.rdata = checkNotNull(rdata, "rdata of %s", name);
        
        checkArgument(name.length() <= 255, "Name must be limited to 255 characters"); 
        if (ttl.isPresent()) {
            checkArgument(ttl.get().longValue() <= 0x7FFFFFFFL, // Per RFC 2181 
                "Invalid ttl value: %s, must be 0-2147483647", ttl.get());
        }
    }

    /**
     * an owner name, i.e., the name of the node to which this resource record
     * pertains.
     */
    public String getName() {
        return name;
    }

    /**
     * The mnemonic type of the record. ex {@code CNAME}
     */
    public String getType() {
        return type;
    }

    /**
     * the time interval that the resource record may be cached. Zero implies it
     * is not cached. Absent means default for the zone.
     */
    public Optional<UnsignedInteger> getTTL() {
        return ttl;
    }

    @Override
    protected ImmutableList<D> delegate() {
        return rdata;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ResourceRecordSet))
            return false;
        ResourceRecordSet<?> that = ResourceRecordSet.class.cast(obj);
        return equal(this.name, that.name) && equal(this.type, that.type);
    }

    @Override
    public String toString() {
        return toStringHelper(this).omitNullValues().add("name", name).add("type", type).add("ttl", ttl)
                .add("rdata", rdata).toString();
    }

    public static <D extends Map<String, Object>> Builder<D> builder() {
        return new Builder<D>();
    }

    /**
     * Allows creation or mutation of record sets based on the portable RData
     * form {@code D} as extends {@code Map<String, Object>}
     * 
     * @param <D>
     *            RData type shared across elements. see
     *            {@link denominator.model.rdata}
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
            this.rdata = ImmutableList.<D> builder().addAll(ImmutableList.copyOf(checkNotNull(rdata, "rdata")));
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
        public Builder<D> addAll(Iterable<D> rdata) {
            this.rdata = ImmutableList.<D> builder().addAll(checkNotNull(rdata, "rdata"));
            return this;
        }

        @Override
        protected ImmutableList<D> rdataValues() {
            return rdata.build();
        }
    }
}