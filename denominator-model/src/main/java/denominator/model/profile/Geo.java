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
            // TODO: remove group from geo in 2.0
            if (profile.containsKey("group"))
                return new Geo(profile.get("group").toString(), builder.build());
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
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #create(Multimap)}
     */
    @Deprecated
    public static Geo create(String group, Multimap<String, String> regions) {
        return new Geo(group, regions);
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

    @Deprecated
    @ConstructorProperties({ "group", "regions" })
    private Geo(String group, Multimap<String, String> regionsAsMultimap) {
        checkNotNull(regionsAsMultimap, "regions");
        checkNotNull(group, "group");
        this.regionsAsMultimap = ImmutableMultimap.copyOf(regionsAsMultimap);
        this.regions = regionsAsMultimap.asMap();
        this.delegate = ImmutableMap.<String, Object> builder().put("type", type).put("group", group)
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
