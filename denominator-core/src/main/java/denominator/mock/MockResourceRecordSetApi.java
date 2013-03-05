package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.primitives.UnsignedInteger.fromIntBits;
import static denominator.model.ResourceRecordSets.nameAndType;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

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
     * adds a bunch of fake records
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return data.get(zoneName).iterator();
    }

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = from(data.get(zoneName))
                .firstMatch(nameAndType(rrset.getName(), rrset.getType()));
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
    public void remove(ResourceRecordSet<?> rrset) {
        checkNotNull(rrset, "rrset was null");
        Optional<ResourceRecordSet<?>> rrsMatch = from(data.get(zoneName))
                .firstMatch(nameAndType(rrset.getName(), rrset.getType()));
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