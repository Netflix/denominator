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

public class MockAllProfileResourceRecordSetApi implements denominator.AllProfileResourceRecordSetApi {

    protected final Multimap<String, ResourceRecordSet<?>> records;
    protected final String zoneName;

    MockAllProfileResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> records, String zoneName) {
        this.records = records;
        this.zoneName = zoneName;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return from(records.get(zoneName))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        checkNotNull(name, "name");
        return from(records.get(zoneName))
                .filter(nameEqualTo(name))
                .toSortedList(usingToString())
                .iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(records.get(zoneName))
                .filter(nameAndTypeEqualTo(name, type))
                .toSortedList(usingToString())
                .iterator();
    }

    static Predicate<ResourceRecordSet<?>> nameAndTypeEqualTo(String name, String type) {
        return and(nameEqualTo(name), typeEqualTo(type));
    }

    static class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> records;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> records) {
            this.records = Multimap.class.cast(records);
        }

        @Override
        public AllProfileResourceRecordSetApi create(String zoneName) {
            checkArgument(records.keySet().contains(zoneName), "zone %s not found", zoneName);
            return new MockAllProfileResourceRecordSetApi(records, zoneName);
        }
    }
}
