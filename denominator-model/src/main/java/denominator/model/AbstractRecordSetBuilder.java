package denominator.model;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

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
    private Optional<Integer> ttl = Optional.absent();

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
    public B ttl(Integer ttl) {
        this.ttl = Optional.fromNullable(ttl);
        return (B) this;
    }

    public ResourceRecordSet<D> build() {
        return new ResourceRecordSet<D>(name, type, ttl, rdataValues());
    }

    /**
     * aggregate collected rdata values
     */
    protected abstract ImmutableList<D> rdataValues();
}
