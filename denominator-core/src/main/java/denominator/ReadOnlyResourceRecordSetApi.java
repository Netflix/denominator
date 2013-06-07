package denominator;

import java.util.Iterator;

import com.google.common.annotations.Beta;

import denominator.model.ResourceRecordSet;

@Beta
interface ReadOnlyResourceRecordSetApi extends Iterable<ResourceRecordSet<?>> {

    /**
     * a listing of all resource record sets inside the zone.
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
     * Iterates across all record sets in the zone. Implementations are lazy
     * when possible.
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
     * @return iterator which is lazy where possible, empty if there are no records with that name.
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     */
    Iterator<ResourceRecordSet<?>> listByName(String name);

    /**
     * a listing of all resource record sets by name and type.
     * 
     * @param name
     *            {@link ResourceRecordSet#getName() name} of the rrset
     * @param type
     *            {@link ResourceRecordSet#getType() type} of the rrset
     * 
     * @return iterator which is lazy where possible, empty if there are no records with that name.
     * @throws IllegalArgumentException
     *             if the zone {@code idOrName} is not found.
     */
    Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type);
}
