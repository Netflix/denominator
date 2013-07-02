package denominator.mock;

import static denominator.common.Util.and;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.withoutProfile;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

public final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final SortedSet<ResourceRecordSet<?>> records;

    MockResourceRecordSetApi(SortedSet<ResourceRecordSet<?>> records) {
        this.records = records;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
        return filter(records.iterator(), withoutProfile());
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        return filter(records.iterator(), and(nameEqualTo(name), withoutProfile()));
    }

    @Override
    public ResourceRecordSet<?> getByNameAndType(String name, String type) {
        return nextOrNull(filter(records.iterator(), and(nameAndTypeEqualTo(name, type), withoutProfile())));
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        ResourceRecordSet<?> rrsMatch = getByNameAndType(rrset.name(), rrset.type());
        if (rrsMatch != null) {
            records.remove(rrsMatch);
        }
        records.add(rrset);
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        ResourceRecordSet<?> rrsMatch = getByNameAndType(name, type);
        if (rrsMatch != null) {
            records.remove(rrsMatch);
        }
    }

    public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Map<Zone, SortedSet<ResourceRecordSet<?>>> records;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Map<Zone, SortedSet<ResourceRecordSet>> records) {
            this.records = Map.class.cast(records);
        }

        @Override
        public ResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return new MockResourceRecordSetApi(records.get(zone));
        }
    }
}