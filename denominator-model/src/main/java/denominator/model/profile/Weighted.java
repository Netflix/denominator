package denominator.model.profile;

import static denominator.common.Preconditions.checkArgument;
import static denominator.model.ResourceRecordSets.tryFindProfile;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.model.ResourceRecordSet;

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
public class Weighted extends LinkedHashMap<String, Object> {

    /**
     * returns a Weighted view of the {@code profile} or null if no weighted
     * profile found.
     * 
     * @since 1.3.1
     */
    public static Weighted asWeighted(Map<String, Object> profile) {
        if (profile == null)
            return null;
        if (profile instanceof Weighted)
            return Weighted.class.cast(profile);
        return new Weighted(Integer.class.cast(profile.get("weight")));
    }

    /**
     * returns a Weighted view of the {@code rrset} or null if no weighted
     * profile found.
     * 
     * @since 1.3.1
     */
    public static Weighted asWeighted(ResourceRecordSet<?> rrset) {
        return asWeighted(tryFindProfile(rrset, "weighted"));
    }

    /**
     * @param weight
     *            corresponds to {@link #weight()}
     */
    public static Weighted create(int weight) {
        return new Weighted(weight);
    }

    @ConstructorProperties({ "weight" })
    private Weighted(int weight) {
        checkArgument(weight >= 0, "weight must be positive");
        put("type", "weighted");
        put("weight", weight);
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
        return Integer.class.cast(get("weight"));
    }

    private static final long serialVersionUID = 1L;
}
