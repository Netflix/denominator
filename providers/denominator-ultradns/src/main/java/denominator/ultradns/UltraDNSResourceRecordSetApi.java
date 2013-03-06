package denominator.ultradns;

import static com.google.common.collect.Ordering.usingToString;
import static denominator.ultradns.GroupByRecordNameAndTypeIterator.parseRdataList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.domain.ResourceRecordMetadata;
import org.jclouds.ultradns.ws.features.ResourceRecordApi;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedInteger;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
public final class UltraDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {
    static final class Factory implements denominator.ResourceRecordSetApi.Factory {

        private final UltraDNSWSApi api;

        @Inject
        Factory(UltraDNSWSApi api) {
            this.api = api;
        }

        @Override
        public ResourceRecordSetApi create(final String zoneName) {
            return new UltraDNSResourceRecordSetApi(api.getResourceRecordApiForZone(zoneName));
        }
    }

    private final ResourceRecordApi api;

    UltraDNSResourceRecordSetApi(ResourceRecordApi api) {
        this.api = api;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> list() {
        Iterator<ResourceRecordMetadata> orderedRecords = api.list().toSortedList(usingToString()).iterator();
        return new GroupByRecordNameAndTypeIterator(orderedRecords);
    }

    @Override
    public Optional<ResourceRecordSet<?>> getByNameAndType(String name, String type) {
        List<ResourceRecordMetadata> existingRecords = api.listByNameAndType(name, type).toSortedList(usingToString());
        if (existingRecords.isEmpty())
            return Optional.absent();
        
        Optional<UnsignedInteger> ttl = Optional.absent();
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(name)
                                                                .type(type);

        for (ResourceRecordMetadata existingRecord : existingRecords) {
            if (!ttl.isPresent())
                ttl = Optional.of(existingRecord.getRecord().getTTL());
            builder.add(parseRdataList(type, existingRecord.getRecord().getRData()));
        }
        return Optional.<ResourceRecordSet<?>> of(builder.ttl(ttl.get()).build());
    }

    @Override
    public void add(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void remove(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void replace(ResourceRecordSet<?> rrset) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void applyTTLToNameAndType(UnsignedInteger ttl, String name, String type) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
