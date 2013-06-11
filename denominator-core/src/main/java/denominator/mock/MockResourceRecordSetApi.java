package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Multimaps.filterValues;
import static com.google.common.collect.Ordering.usingToString;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static denominator.model.ResourceRecordSets.withoutProfile;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.Zone;


public final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final Multimap<Zone, ResourceRecordSet<?>> records;
    private final Zone zone;

    MockResourceRecordSetApi(Multimap<Zone, ResourceRecordSet<?>> records, Zone zone) {
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
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(records.get(zone)).firstMatch(and(nameEqualTo(name), typeEqualTo(type)));
    }

    @Override
    @Deprecated
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        return iterateByName(name);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
        checkNotNull(name, "name");
        return from(records.get(zone)).filter(nameEqualTo(name)).iterator();
    }

    @Override
    public void applyTTLToNameAndType(int ttl, String name, String type) {
        checkNotNull(ttl, "ttl");
        Optional<ResourceRecordSet<?>> existing = getByNameAndType(name, type);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        if (rrset.ttl().isPresent() && rrset.ttl().get().equals(ttl))
            return;
        ResourceRecordSet<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>> builder()
                                                                       .name(rrset.name())
                                                                       .type(rrset.type())
                                                                       .ttl(ttl)
                                                                       .addAll(rrset).build();
        replace(rrs);
    }

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.name(), rrset.type());
        Builder<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>>builder()
                                                             .name(rrset.name())
                                                             .type(rrset.type())
                                                             .ttl(rrset.ttl().or(3600));
        if (rrsMatch.isPresent()) {
            rrs.addAll(rrsMatch.get());
            rrs.addAll(filter(rrset, not(in(rrsMatch.get()))));
            records.remove(zone, rrsMatch.get());
        } else {
            rrs.addAll(rrset);
        }
        records.put(zone, rrs.build());
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.name(), rrset.type());
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
        records.put(zone, rrset);
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.name(), rrset.type());
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
            if (rrsMatch.get().rdata().size() > 1) {
                records.put(zone, ResourceRecordSet.<Map<String, Object>> builder()
                                                    .name(rrset.name())
                                                    .type(rrset.type())
                                                    .ttl(rrsMatch.get().ttl().get())
                                                    .addAll(filter(rrsMatch.get(), not(in(rrset))))
                                                    .build());
            }
        }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(name, type);
        if (rrsMatch.isPresent()) {
            records.remove(zone, rrsMatch.get());
        }
    }

    public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Multimap<Zone, ResourceRecordSet<?>> records;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<Zone, ResourceRecordSet> records) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), withoutProfile()));
        }

        @Override
        public ResourceRecordSetApi create(String idOrName) {
            Zone zone = Zone.create(idOrName);
            checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
            return new MockResourceRecordSetApi(records, zone);
        }
    }
}