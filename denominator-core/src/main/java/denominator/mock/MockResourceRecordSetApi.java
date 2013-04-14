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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;


public final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {

    private final Multimap<String, ResourceRecordSet<?>> records;
    private final String zoneName;

    MockResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> records, String zoneName) {
        this.records = records;
        this.zoneName = zoneName;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return FluentIterable.from(records.get(zoneName)).toSortedList(usingToString()).iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(records.get(zoneName)).firstMatch(and(nameEqualTo(name), typeEqualTo(type)));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> listByName(String name) {
        checkNotNull(name, "name");
        return from(records.get(zoneName)).filter(nameEqualTo(name)).iterator();
    }

    @Override
    public void applyTTLToNameAndType(int ttl, String name, String type) {
        checkNotNull(ttl, "ttl");
        Optional<ResourceRecordSet<?>> existing = getByNameAndType(name, type);
        if (!existing.isPresent())
            return;
        ResourceRecordSet<?> rrset = existing.get();
        if (rrset.getTTL().isPresent() && rrset.getTTL().get().equals(ttl))
            return;
        ResourceRecordSet<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>> builder()
                                                                       .name(rrset.getName())
                                                                       .type(rrset.getType())
                                                                       .ttl(ttl)
                                                                       .addAll(rrset).build();
        replace(rrs);
    }

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.getName(), rrset.getType());
        Builder<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>>builder()
                                                             .name(rrset.getName())
                                                             .type(rrset.getType())
                                                             .ttl(rrset.getTTL().or(3600));
        if (rrsMatch.isPresent()) {
            rrs.addAll(rrsMatch.get());
            rrs.addAll(filter(rrset, not(in(rrsMatch.get()))));
            records.remove(zoneName, rrsMatch.get());
        } else {
            rrs.addAll(rrset);
        }
        records.put(zoneName, rrs.build());
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.getName(), rrset.getType());
        if (rrsMatch.isPresent()) {
            records.remove(zoneName, rrsMatch.get());
        }
        records.put(zoneName, rrset);
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.getName(), rrset.getType());
        if (rrsMatch.isPresent()) {
            records.remove(zoneName, rrsMatch.get());
            if (rrsMatch.get().size() > 1) {
                records.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                                                    .name(rrset.getName())
                                                    .type(rrset.getType())
                                                    .ttl(rrsMatch.get().getTTL().get())
                                                    .addAll(filter(rrsMatch.get(), not(in(rrset))))
                                                    .build());
            }
        }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(name, type);
        if (rrsMatch.isPresent()) {
            records.remove(zoneName, rrsMatch.get());
        }
    }

    public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> records;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> records) {
            this.records = Multimap.class.cast(filterValues(Multimap.class.cast(records), withoutProfile()));
        }

        @Override
        public ResourceRecordSetApi create(String zoneName) {
            checkArgument(records.keySet().contains(zoneName), "zone %s not found", zoneName);
            return new MockResourceRecordSetApi(records, zoneName);
        }
    }
}