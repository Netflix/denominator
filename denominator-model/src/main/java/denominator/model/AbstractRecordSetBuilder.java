package denominator.model;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedInteger;

/**
 * Capable of building record sets from rdata input types expressed as {@code E}
 * 
 * @param <E>
 *            input type of rdata
 * @param <D>
 *            portable type of the rdata in the {@link ResourceRecordSet}
 */
abstract class AbstractRecordSetBuilder<E, D extends Map<String, Object>, B extends AbstractRecordSetBuilder<E, D, B>> {

    private String name;
    private String type;
    private Optional<UnsignedInteger> ttl = Optional.absent();

    /**
     * @see ResourceRecordSet#getName()
     */
    @SuppressWarnings("unchecked")
    public B name(String name) {
        this.name = name;
        return (B) this;
    }

    /**
     * @see ResourceRecordSet#getType()
     */
    @SuppressWarnings("unchecked")
    public B type(String type) {
        this.type = type;
        return (B) this;
    }

    /**
     * @see ResourceRecordSet#getTTL()
     */
    @SuppressWarnings("unchecked")
    public B ttl(UnsignedInteger ttl) {
        this.ttl = Optional.fromNullable(ttl);
        return (B) this;
    }

    /**
     * @see ResourceRecordSet#getTTL()
     */
    public B ttl(int ttl) {
        return ttl(UnsignedInteger.fromIntBits(ttl));
    }

    public ResourceRecordSet<D> build() {
        return new ResourceRecordSet<D>(name, type, ttl, rdataValues());
    }

    /**
     * aggregate collected rdata values
     */
    protected abstract ImmutableList<D> rdataValues();
}