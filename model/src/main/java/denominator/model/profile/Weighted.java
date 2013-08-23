package denominator.model.profile;

import static denominator.common.Preconditions.checkArgument;

import java.beans.ConstructorProperties;

/**
 * Record sets with this profile are load balanced, differentiated by an integer
 * {@code weight}.
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * Weighted profile = Weighted.create(2);
 * </pre>
 * 
 * @since 1.3
 */
public class Weighted {

    /**
     * @param weight
     *            corresponds to {@link #weight()}
     */
    public static Weighted create(int weight) {
        return new Weighted(weight);
    }

    private final int weight;

    @ConstructorProperties({ "weight" })
    private Weighted(int weight) {
        checkArgument(weight >= 0, "weight must be positive");
        this.weight = weight;
    }

    /**
     * 
     * {@code 0} to always serve the record. Otherwise, provider-specific range
     * of positive numbers which differentiate the load to send to this record
     * set vs another.
     * 
     * <br>
     * <br>
     * <b>Note</b><br>
     * 
     * In some implementation, such as UltraDNS, only even number weights are
     * supported! For highest portability, use even numbers between
     * {@code 0-100}
     * 
     */
    public int weight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Weighted))
            return false;
        Weighted that = Weighted.class.cast(o);
        return weight() == that.weight();
    }

    @Override
    public int hashCode() {
        return 37 * 17 + weight();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Weighted [weight=").append(weight()).append("]").toString();
    }
}
