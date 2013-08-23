package denominator.model.profile;

import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.Collections;
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
public class Geo {

    /**
     * @param regions
     *            corresponds to {@link #regions()}
     * 
     * @since 1.3
     */
    public static Geo create(Map<String, Collection<String>> regions) {
        return new Geo(regions);
    }

    private final Map<String, Collection<String>> regions;

    @ConstructorProperties({ "regions" })
    private Geo(Map<String, Collection<String>> regions) {
        this.regions = Collections.unmodifiableMap(checkNotNull(regions, "regions"));
    }

    /**
     * a filtered view of
     * {@code denominator.profile.GeoResourceRecordSetApi.supportedRegions()} ,
     * which describes the traffic desired for this profile.
     * 
     * @since 1.3
     */
    public Map<String, Collection<String>> regions() {
        return regions;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Geo))
            return false;
        Geo that = Geo.class.cast(o);
        return regions().equals(that.regions());
    }

    @Override
    public int hashCode() {
        return regions().hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Geo [regions=").append(regions()).append("]").toString();
    }
}
