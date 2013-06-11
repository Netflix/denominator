package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;

import java.util.Iterator;

import javax.inject.Inject;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;

import denominator.AllProfileResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public class MockAllProfileResourceRecordSetApi implements denominator.AllProfileResourceRecordSetApi {

    protected final Multimap<Zone, ResourceRecordSet<?>> records;
    protected final Zone zone;

    MockAllProfileResourceRecordSetApi(Multimap<Zone, ResourceRecordSet<?>> records, Zone zone) {
        this.records = records;
        this.zone = zone;
    }

    @Deprecated
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return iterator();
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return usingToString().immutableSortedCopy(records.get(zone)).iterator();
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return iterateByName(name);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name");
        return from(records.get(zone))
                .filter(nameEqualTo(name))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
        return iterateByNameAndType(name, type);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(records.get(zone))
                .filter(nameAndTypeEqualTo(name, type))
                .toSortedList(usingToString())
                .iterator();
    }

    static Predicate<ResourceRecordSet<?>> nameAndTypeEqualTo(String name, String type) {
        return and(nameEqualTo(name), typeEqualTo(type));
    }

    static class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

        private final Multimap<Zone, ResourceRecordSet<?>> records;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<Zone, ResourceRecordSet> records) {
            this.records = Multimap.class.cast(records);
        }

        @Override
        public AllProfileResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return new MockAllProfileResourceRecordSetApi(records, zone);
        }
    }
}
