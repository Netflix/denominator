package denominator.mock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.FluentIterable.from;
import static denominator.model.ResourceRecordSets.containsRData;
import static denominator.model.ResourceRecordSets.nameAndType;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Optional;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Iterables.*;
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
     * adds a bunch of fake records
     */
    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        return data.get(zoneName).iterator();
    }

    @Override
    public void add(String name, String type, UnsignedInteger ttl, Map<String, Object> rdata) {
        Optional<ResourceRecordSet<?>> rrsMatch = from(data.get(zoneName)).firstMatch(nameAndType(name, type));
        Builder<Map<String, Object>> rrs  = ResourceRecordSet.<Map<String, Object>>builder()
                                                             .name(name)
                                                             .type(type)
                                                             .ttl(ttl);
        if (rrsMatch.isPresent()) {
            rrs.addAll(rrsMatch.get());
            if (!rrsMatch.get().contains(rdata))
                rrs.add(rdata);
            data.remove(zoneName, rrsMatch.get());
        } else {
            rrs.add(rdata);
        }
        data.put(zoneName, rrs.build());
    }

    @Override
    public void add(String name, String type, Map<String, Object> rdata) {
        add(name, type, UnsignedInteger.fromIntBits(3600), rdata);
    }

    @Override
    public void remove(String name, String type, Map<String, Object> rdata) {
        Optional<ResourceRecordSet<?>> rrsMatch = from(data.get(zoneName)).firstMatch(
                and(nameAndType(name, type), containsRData(rdata)));
        if (rrsMatch.isPresent()) {
            data.remove(zoneName, rrsMatch.get());
            if (rrsMatch.get().size() > 1) {
                data.put(zoneName, ResourceRecordSet.<Map<String, Object>> builder()
                                                    .name(name)
                                                    .type(type)
                                                    .ttl(rrsMatch.get().getTTL().orNull())
                                                    .addAll(filter(rrsMatch.get(), not(equalTo(rdata))))
                                                    .build());
            }
        }
    }
}