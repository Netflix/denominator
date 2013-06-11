package denominator.model.profile;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;

/**
 * Record sets with this profile are visible to the regions specified.
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * Geo profile = Geo.create(ImmutableMultimap.of(&quot;United States (US)&quot;, &quot;Maryland&quot;));
 * </pre>
 */
public class Geo extends ForwardingMap<String, Object> {

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #create(Multimap)}
     */
    @Deprecated
    public static Geo create(String group, Multimap<String, String> regions) {
        return new Geo(group, regions);
    }

    /**
     * @param regions corresponds to {@link #regions()}
     * 
     * @since 1.3
     */
    public static Geo create(Multimap<String, String> regions) {
        return new Geo(regions);
    }

    private final String type = "geo";
    private final Multimap<String, String> regions;

    @ConstructorProperties({ "regions"})
    private Geo(Multimap<String, String> regions) {
        checkNotNull(regions, "regions");
        this.regions = ImmutableMultimap.copyOf(regions);
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("type", type)
                                    .put("regions", regions).build();
    }

    @Deprecated
    @ConstructorProperties({ "group", "regions" })
    private Geo(String group, Multimap<String, String> regions) {
        checkNotNull(regions, "regions");
        checkNotNull(group, "group");
        this.regions = ImmutableMultimap.copyOf(regions);
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("type", type)
                                    .put("group", group)
                                    .put("regions", regions).build();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link ResourceRecordSet#qualifier() qualifier}
     */
    @Deprecated
    public String getGroup() {
        return group();
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link ResourceRecordSet#qualifier() qualifier}
     */
    @Deprecated
    public String group() {
        return (String) delegate.get("group");
    }

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #regions()}
     */
    @Deprecated
    public Multimap<String, String> getRegions() {
        return regions();
    }

    /**
     * a filtered view of
     * {@code denominator.profile.GeoResourceRecordSetApi.supportedRegions()}
     * , which describes the traffic desired for this profile.
     * 
     * @since 1.3
     */
    public Multimap<String, String> regions() {
        return regions;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
