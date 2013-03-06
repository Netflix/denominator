package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Ordering.usingToString;
import static com.google.common.primitives.UnsignedInteger.fromIntBits;
import static denominator.model.ResourceRecordSets.nameAndType;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.google.common.primitives.UnsignedInteger;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;


public final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final Multimap<String, ResourceRecordSet<?>> data;

        // wildcard types are not currently injectable in dagger
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Inject
        Factory(Multimap<String, ResourceRecordSet> data) {
            this.data = Multimap.class.cast(data);
        }

        @Override
        public ResourceRecordSetApi create(String zoneName) {
            checkArgument(data.keySet().contains(zoneName), "zone %s not found", zoneName);
            return new MockResourceRecordSetApi(data, zoneName);
        }
    }

    private final Multimap<String, ResourceRecordSet<?>> data;
    private final String zoneName;

    MockResourceRecordSetApi(Multimap<String, ResourceRecordSet<?>> data, String zoneName) {
        this.data = data;
        this.zoneName = zoneName;
    }

    /**
     * sorted to help tests from breaking
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return FluentIterable.from(data.get(zoneName)).toSortedList(usingToString()).iterator();
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return from(data.get(zoneName)).firstMatch(nameAndType(name, type));
    }

    @Override
    public void applyTTLToNameAndType(UnsignedInteger ttl, String name, String type) {
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
                                                             .ttl(rrset.getTTL().or(fromIntBits(3600)));
        if (rrsMatch.isPresent()) {
            rrs.addAll(rrsMatch.get());
            rrs.addAll(filter(rrset, not(in(rrsMatch.get()))));
            data.remove(zoneName, rrsMatch.get());
        } else {
            rrs.addAll(rrset);
        }
        data.put(zoneName, rrs.build());
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.getName(), rrset.getType());
        if (rrsMatch.isPresent()) {
            data.remove(zoneName, rrsMatch.get());
        }
        data.put(zoneName, rrset);
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = getByNameAndType(rrset.getName(), rrset.getType());
        if (rrsMatch.isPresent()) {
            data.remove(zoneName, rrsMatch.get());
            if (rrsMatch.get().size() > 1) {
                data.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                                                    .name(rrset.getName())
                                                    .type(rrset.getType())
                                                    .ttl(rrsMatch.get().getTTL().get())
                                                    .addAll(filter(rrsMatch.get(), not(in(rrset))))
                                                    .build());
            }
        }
    }
}