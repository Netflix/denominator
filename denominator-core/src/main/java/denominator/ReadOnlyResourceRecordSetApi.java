package denominator;

import java.util.Iterator;

import com.google.common.annotations.Beta;

import denominator.model.ResourceRecordSet;

@Beta
interface ReadOnlyResourceRecordSetApi {

    /**
     * a listing of all resource record sets inside the zone.
     * 
     * @return iterator which is lazy where possible
     * @throws IllegalArgumentException
     *             if the {@code zoneName} is not found.
     */
    Iterator<ResourceRecordSet<?>> list();

    /**
     * a listing of all resource record sets which have the specified name.
     * 
     * @return iterator which is lazy where possible, empty if there are no records with that name.
     * @throws IllegalArgumentException
     *             if the {@code zoneName} is not found.
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
     *             if the {@code zoneName} is not found.
     */
    Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type);
}
