package denominator.model;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * A zone is a delegated portion of DNS. We use the word {@code zone} instead of
 * {@code domain}, as denominator focuses on configuration aspects of DNS.
 * 
 * @since 1.2
 * See <a href="http://www.ietf.org/rfc/rfc1035.txt">RFC 1035</a>
 */
public class Zone {
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
        return new Zone(name, Optional.fromNullable(id));
    }

    private final String name;
    private final Optional<String> id;

    @ConstructorProperties({ "name", "id" })
    Zone(String name, Optional<String> id) {
        this.name = checkNotNull(name, "name");
        this.id = checkNotNull(id, "id of %s", name);
    }

    /**
     * The origin or starting point for the zone in the DNS tree. Usually
     * includes a trailing dot, ex. "{@code netflix.com.}"
     */
    public String name() {
        return name;
    }

    /**
     * When present, the service supports multiple zones with the same
     * {@link #name}. When absent, it doesn't. The value is likely to have been
     * system generated. Even if a provider has an id associated with a zone, if
     * it isn't used by their api calls, this method will return absent.
     * 
     * @see #idOrName()
     */
    public Optional<String> id() {
        return id;
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
        return id().or(name());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof Zone))
            return false;
        Zone that = Zone.class.cast(obj);
        return equal(this.name, that.name) && equal(this.id, that.id);
    }

    @Override
    public String toString() {
        return toStringHelper(this).omitNullValues().add("name", name).add("id", id.orNull()).toString();
    }
}
