package denominator;

import java.util.Iterator;

import com.google.common.base.Optional;

import denominator.model.ResourceRecordSet;

public interface ResourceRecordSetApi extends Iterable<ResourceRecordSet<?>> {

    /**
     * Iterates across all basic record sets in the zone (those with no
     * {@link ResourceRecordSet#profiles() profile}). Implementations are
     * lazy when possible.
     * 
     * @return iterator which is lazy where possible
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     */
    @Override
    Iterator<ResourceRecordSet<?>> iterator();

    /**
     * a listing of all resource record sets which have the specified name.
     * 
     * @return iterator which is lazy where possible, empty if there are no
     *         records with that name.
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     * @since 1.3
     */
    Iterator<ResourceRecordSet<?>> iterateByName(String name);

    /**
     * retrieve a resource record set by name and type.
     * 
     * @param name
     *            {@link ResourceRecordSet#name() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#type() type} of the rrset
     * 
     * @return present if a resource record exists with the same {@code name}
     *         and {@code type}
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     */
    Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type);

    /**
     * Idempotently replaces any existing records with
     * {@link ResourceRecordSet#name() name} and
     * {@link ResourceRecordSet#type()} corresponding to {@code rrset}. If no
     * records exists, they will be added.
     * 
     * <p/>
     * Example of replacing the {@code A} record set for
     * {@code www.denominator.io.}:
     * 
     * <pre>
     * rrsApi.put(a(&quot;www.denominator.io.&quot;, &quot;192.0.2.1&quot;));
     * </pre>
     * 
     * @param rrset
     *            contains the {@code rdata} elements ensure exist. If
     *            {@link ResourceRecordSet#ttl() ttl} is not present, zone
     *            default is used.
     * 
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found
     * @since 1.3
     */
    void put(ResourceRecordSet<?> rrset);

    /**
     * deletes a resource record set by name and type idempotently. This does
     * not error if the record set doesn't exist.
     * 
     * @param name
     *            {@link ResourceRecordSet#name() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#type() type} of the rrset
     * 
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     */
    void deleteByNameAndType(String name, String type);

    static interface Factory {
        ResourceRecordSetApi create(String idOrName);
    }

    /**
     * a listing of all basic resource record sets inside the zone.
     * 
     * @return iterator which is lazy where possible
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     * 
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #iterator}
     */
    @Deprecated
    Iterator<ResourceRecordSet<?>> list();

    /**
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #iterateByName(String)}
     */
    @Deprecated
    Iterator<ResourceRecordSet<?>> listByName(String name);

    /**
     * If a {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#name() name} and
     * {@link ResourceRecordSet#type() type} corresponding to {@code rrset},
     * this adds the {@code rdata} to that set. Otherwise, it creates a
     * {@link ResourceRecordSet} initially comprised of the specified
     * {@code rrset}.
     * 
     * Example of adding "192.0.2.1" to the {@code A} record set for
     * {@code www.denominator.io.}
     * 
     * <pre>
     * import static denominator.model.ResourceRecordSets.a;
     * ...
     * rrsApi.add(a("www.denominator.io.", 3600, "192.0.2.1"));
     * </pre>
     * 
     * @param rrset
     *            contains the {@code rdata} elements to be added. If
     *            {@link ResourceRecordSet#ttl() ttl} is present, it will
     *            replace the TTL on all records.
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #put(ResourceRecordSet)}
     */
    @Deprecated
    void add(ResourceRecordSet<?> rrset);

    /**
     * Ensures the supplied {@code ttl} is uniform for all record sets with the
     * supplied {@link ResourceRecordSet#name() name} and
     * {@link ResourceRecordSet#type() type}. Returns without error if there
     * are no record sets of the specified name and type.
     * 
     * @param ttl
     *            ttl to apply to all records in seconds
     * @param name
     *            {@link ResourceRecordSet#name() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#type() type} of the rrset
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #put(ResourceRecordSet)}
     */
    @Deprecated
    void applyTTLToNameAndType(int ttl, String name, String type);

    /**
     * Idempotently replaces any existing records with
     * {@link ResourceRecordSet#name() name} and
     * {@link ResourceRecordSet#type() type} corresponding to {@code rrset}.
     * 
     * Example of replacing the {@code A} record set for
     * {@code www.denominator.io.} with "192.0.2.1".
     * 
     * <pre>
     * import static denominator.model.ResourceRecordSets.a;
     * ...
     * rrsApi.replace(a("www.denominator.io.", 3600, "192.0.2.1"));
     * </pre>
     * 
     * @param rrset
     *            contains the {@code rdata} elements ensure exist. If
     *            {@link ResourceRecordSet#ttl() ttl} is not present, zone
     *            default is used.
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #put(ResourceRecordSet)}
     */
    @Deprecated
    void replace(ResourceRecordSet<?> rrset);

    /**
     * If a {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#name() name} and
     * {@link ResourceRecordSet#type() type} corresponding to {@code rrset},
     * remove values corresponding to input {@code rdata}, or removes the set
     * entirely, if this is the only entry.
     * 
     * Example of removing "192.0.2.1" from the {@code A} record set for
     * {@code www.denominator.io.}
     * 
     * <pre>
     * import static denominator.model.ResourceRecordSets.a;
     * ...
     * rrsApi.remove(a("www.denominator.io.", "192.0.2.1"));
     * </pre>
     * 
     * @param rrset
     *            contains the {@code rdata} elements to be removed. The
     *            {@link ResourceRecordSet#ttl() ttl} is ignored.
     * @deprecated Will be removed in denominator 2.0. Please use
     *             {@link #put(ResourceRecordSet)}
     */
    @Deprecated
    void remove(ResourceRecordSet<?> rrset);
}
