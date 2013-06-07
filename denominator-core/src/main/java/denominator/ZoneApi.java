package denominator;

import java.util.Iterator;

import denominator.model.Zone;

public interface ZoneApi extends Iterable<Zone> {
    /**
     * a listing of all zone names, including trailing dot. ex.
     * {@code netflix.com.}. Implementations are lazy when possible.
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #iterator}
     */
    @Deprecated
    Iterator<String> list();

    /**
     * Iterates across all zones, returning their name and id (when present).
     * Implementations are lazy when possible.
     */
    @Override
    Iterator<Zone> iterator();
}
