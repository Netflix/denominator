package denominator;

import java.util.Iterator;
import java.util.Map;

import com.google.common.primitives.UnsignedInteger;

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

    /**
     * If a {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#getName() name} and
     * {@link ResourceRecordSet#getName() type} corresponding to parameters,
     * this adds the {@code rdata} to that set. Otherwise, it creates a
     * {@link ResourceRecordSet} initially comprised of the specified arguments.
     * 
     * @param name
     *            the {@link ResourceRecordSet#getName() name} of the record set
     * @param type
     *            the {@link ResourceRecordSet#getType() type} of the record set
     * @param ttl
     *            the {@link ResourceRecordSet#getTTL() ttl} of the record set
     * @param rdata
     *            the rdata to add to the record set
     */
    void add(String name, String type, UnsignedInteger ttl, Map<String, Object> rdata);

    /**
     * like {@link #add(String, String, UnsignedInteger, Map)}, except using the
     * default ttl for the zone.
     */
    void add(String name, String type, Map<String, Object> rdata);

    /**
     * If a {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#getName() name} and
     * {@link ResourceRecordSet#getName() type} corresponding to parameters,
     * this either removes the value corresponding to the {@code rdata}, or
     * removes the set entirely, if this is the only entry.
     * 
     * @param name
     *            the {@link ResourceRecordSet#getName() name} of the record set
     * @param type
     *            the {@link ResourceRecordSet#getType() type} of the record set
     * @param rdata
     *            the rdata to remove from the record set
     */
    void remove(String name, String type, Map<String, Object> rdata);
}
