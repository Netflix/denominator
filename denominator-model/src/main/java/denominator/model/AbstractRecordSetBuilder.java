package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private ImmutableList.Builder<Map<String, Object>> profile = ImmutableList.builder();

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

    /**
     * adds a value to the builder.
     * 
     * ex.
     * 
     * <pre>
     * builder.addProfile(geo);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public B addProfile(Map<String, Object> profile) {
        this.profile.add(checkNotNull(profile, "profile"));
        return (B) this;
    }

    /**
     * adds profile values in the builder
     * 
     * ex.
     * 
     * <pre>
     * 
     * builder.addAllProfile(otherProfile);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public B addAllProfile(Iterable<Map<String, Object>> profile) {
        this.profile.addAll(checkNotNull(profile, "profile"));
        return (B) this;
    }

    /**
     * @see ResourceRecordSet#getProfiles()
     */
    @SuppressWarnings("unchecked")
    public B profile(Iterable<Map<String, Object>> profile) {
        this.profile = ImmutableList.<Map<String, Object>> builder().addAll(profile);
        return (B) this;
    }

    public ResourceRecordSet<D> build() {
        return new ResourceRecordSet<D>(name, type, ttl, rdataValues(), profile.build());
    }

    /**
     * aggregate collected rdata values
     */
    protected abstract ImmutableList<D> rdataValues();
}
