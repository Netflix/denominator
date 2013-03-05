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

    /**
     * If a {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#getName() name} and
     * {@link ResourceRecordSet#getName() type} corresponding to {@code rrset},
     * this adds the {@code rdata} to that set. Otherwise, it creates a
     * {@link ResourceRecordSet} initially comprised of the specified
     * {@code rrset}.
     * 
     * Example of adding "1.2.3.4" to the {@code A} record set for
     * {@code www.denominator.io.}
     * 
     * <pre>
     * import static denominator.model.ResourceRecordSets.a;
     * ...
     * rrsApi.add(a("www.denominator.io.", 3600, "1.2.3.4"));
     * </pre>
     * 
     * @param rrset
     *            contains the {@code rdata} elements to be added. If
     *            {@link ResourceRecordSet#getTTL() ttl} is present, it will
     *            replace the TTL on all records.
     */
    void add(ResourceRecordSet<?> rrset);

    /**
     * If a {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#getName() name} and
     * {@link ResourceRecordSet#getName() type} corresponding to {@code rrset},
     * remove values corresponding to input {@code rdata}, or removes the set
     * entirely, if this is the only entry.
     * 
     * Example of removing "1.2.3.4" from the {@code A} record set for
     * {@code www.denominator.io.}
     * 
     * <pre>
     * import static denominator.model.ResourceRecordSets.a;
     * ...
     * rrsApi.remove(a("www.denominator.io.", "1.2.3.4"));
     * </pre>
     * 
     * @param rrset
     *            contains the {@code rdata} elements to be removed. The
     *            {@link ResourceRecordSet#getTTL() ttl} is ignored.
     */
    void remove(ResourceRecordSet<?> rrset);
}
