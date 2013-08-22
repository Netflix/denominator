package denominator.model.profile;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.model.ResourceRecordSet;

/**
 * Record sets with this profile are visible to the regions specified.
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * Geo profile = Geo.create(Multimap.of(&quot;United States (US)&quot;, &quot;Maryland&quot;));
 * </pre>
 */
public class Geo extends LinkedHashMap<String, Object> {

    /**
     * returns a Geo view of the {@code profile} or null if no geo profile
     * found.
     * 
     * @since 1.3.1
     * @deprecated will be removed in version 4.0. use {@link #create(Map)}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Geo asGeo(Map<String, Object> profile) {
        if (profile == null)
            return null;
        if (profile instanceof Geo)
            return Geo.class.cast(profile);
        if (profile.get("regions") instanceof Map) {
            Map<?, ?> regions = Map.class.cast(profile.get("regions"));
            for (Map.Entry<?, ?> entry : regions.entrySet()) {
                checkArgument(entry.getValue() instanceof Collection,
                        "expected regions values to be a subtype of Collection<String>, not %s",//
                        entry.getValue().getClass().getSimpleName());
            }
            return new Geo(Map.class.cast(regions));
        } else {
            throw new IllegalArgumentException(
                    "expected profile to have a Map<String, Collection<String>> regions field, not "
                            + profile.get("regions").getClass());
        }
    }

    /**
     * returns a Geo view of the {@code rrset} or null if no geo profile found.
     * 
     * @since 1.3.1
     * @deprecated will be removed in version 4.0. use
     *             {@link ResourceRecordSet#geo()}
     */
    @Deprecated
    public static Geo asGeo(ResourceRecordSet<?> rrset) {
        return rrset.geo();
    }

    /**
     * @param regions
     *            corresponds to {@link #regions()}
     * 
     * @since 1.3
     */
    public static Geo create(Map<String, Collection<String>> regions) {
        return new Geo(regions);
    }

    @ConstructorProperties({ "regions" })
    private Geo(Map<String, Collection<String>> regions) {
        put("type", "geo");
        put("regions", checkNotNull(regions, "regions"));
    }

    /**
     * a filtered view of
     * {@code denominator.profile.GeoResourceRecordSetApi.supportedRegions()} ,
     * which describes the traffic desired for this profile.
     * 
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public Map<String, Collection<String>> regions() {
        return Map.class.cast(get("regions"));
    }

    private static final long serialVersionUID = 1L;
}
