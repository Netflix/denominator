package denominator.model.profile;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static denominator.model.ResourceRecordSets.tryFindProfile;

import java.beans.ConstructorProperties;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
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
     * returns a Geo view of the {@code profile} or null if no geo profile
     * found.
     * 
     * @since 1.3.1
     */
    @SuppressWarnings("unchecked")
    public static Geo asGeo(Map<String, Object> profile) {
        if (profile == null)
            return null;
        if (profile instanceof Geo)
            return Geo.class.cast(profile);
        if (profile.get("regions") instanceof Map) {
            Map<?, ?> regions = Map.class.cast(profile.get("regions"));
            Builder<String, String> builder = ImmutableMultimap.<String, String> builder();
            for (Map.Entry<?, ?> entry : regions.entrySet()) {
                checkArgument(entry.getValue() instanceof Iterable,
                        "expected regions values to be a subtype of Iterable<String>, not %s",//
                        entry.getValue().getClass().getSimpleName());
                builder.putAll(entry.getKey().toString(), Iterable.class.cast(entry.getValue()));
            }
            return new Geo(builder.build());
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
     */
    public static Geo asGeo(ResourceRecordSet<?> rrset) {
        return asGeo(tryFindProfile(rrset, "geo").orNull());
    }

    /**
     * @param regions
     *            corresponds to {@link #regions()}
     * 
     * @since 1.3
     */
    public static Geo create(Multimap<String, String> regions) {
        return new Geo(regions);
    }

    private final String type = "geo";
    private final Map<String, Collection<String>> regions;

    @ConstructorProperties({ "regions" })
    private Geo(Multimap<String, String> regionsAsMultimap) {
        checkNotNull(regionsAsMultimap, "regions");
        this.regionsAsMultimap = ImmutableMultimap.copyOf(regionsAsMultimap);
        this.regions = regionsAsMultimap.asMap();
        this.delegate = ImmutableMap.<String, Object> builder().put("type", type).put("regions", regions).build();
    }

    /**
     * a filtered view of
     * {@code denominator.profile.GeoResourceRecordSetApi.supportedRegions()} ,
     * which describes the traffic desired for this profile.
     * 
     * @since 1.3
     */
    public Multimap<String, String> regions() {
        return regionsAsMultimap;
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    private final transient Multimap<String, String> regionsAsMultimap;

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
