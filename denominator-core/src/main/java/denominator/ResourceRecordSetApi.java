package denominator;

import java.util.Iterator;

import denominator.model.ResourceRecordSet;

public interface ResourceRecordSetApi {

    static interface Factory {
        ResourceRecordSetApi create(String zoneName);
    }

    /**
     * a listing of all resource record sets inside the zone.
     * 
     * @return iterator which is lazy where possible
     * @throws IllegalArgumentException
     *             if the {@code zoneName} is not found.
     */
    Iterator<ResourceRecordSet<?>> list();
}
