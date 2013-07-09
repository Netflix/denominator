package denominator.model;

import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;

/**
 * A zone is a delegated portion of DNS. We use the word {@code zone} instead of
 * {@code domain}, as denominator focuses on configuration aspects of DNS.
 * 
 * @since 1.2 See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class Zone extends LinkedHashMap<String, Object> {

    /**
     * Represent a zone without an {@link #id() id}.
     * 
     * @param name
     *            corresponds to {@link #name()}
     */
    public static Zone create(String name) {
        return create(name, null);
    }

    /**
     * Represent a zone with an {@link #id() id}.
     * 
     * @param name
     *            corresponds to {@link #name()}
     * @param id
     *            nullable; corresponds to {@link #id()}
     */
    public static Zone create(String name, String id) {
        return new Zone(name, id);
    }

    @ConstructorProperties({ "name", "id" })
    Zone(String name, String id) {
        put("name", checkNotNull(name, "name"));
        if (id != null)
            put("id", id);
    }

    @SuppressWarnings("unused")
    private Zone(){
        // for serializers
    }

    /**
     * The origin or starting point for the zone in the DNS tree. Usually
     * includes a trailing dot, ex. "{@code netflix.com.}"
     */
    public String name() {
        return get("name").toString();
    }

    /**
     * When present, the service supports multiple zones with the same
     * {@link #name}. When absent, it doesn't. The value is likely to have been
     * system generated. Even if a provider has an id associated with a zone, if
     * it isn't used by their api calls, this method will return null.
     * 
     * @see #idOrName()
     */
    public String id() {
        return (String) get("id");
    }

    /**
     * It is possible that some zones do not have an id, and in this case the
     * name is used. The following form will ensure you get a reference
     * regardless.
     * 
     * In implementation, this method is the same as calling:
     * {@code zone.id().or(zone.name())}
     * 
     * <br>
     * If {@code denominator.Provider#supportsDuplicateZoneNames()} is true,
     * this will return an id.
     * 
     * @return {@link #id() id} or {@link #name() name} if absent
     */
    public String idOrName() {
        return id() != null ? id() : name();
    }

    private static final long serialVersionUID = 1L;
}
