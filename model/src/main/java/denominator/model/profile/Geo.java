package denominator.model.profile;

import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Record sets with this profile are visible to the regions specified.
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * Geo profile = Geo.create(Map.of(&quot;United States (US)&quot;, Collection.of(&quot;Maryland&quot;)));
 * </pre>
 */
public class Geo extends LinkedHashMap<String, Object> {

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
