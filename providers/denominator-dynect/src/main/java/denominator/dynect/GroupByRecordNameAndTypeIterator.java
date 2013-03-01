package denominator.dynect;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.Iterator;
import java.util.Map;

import org.jclouds.dynect.v3.domain.Record;
import org.jclouds.dynect.v3.domain.RecordId;
import org.jclouds.dynect.v3.features.RecordApi;

import com.google.common.collect.PeekingIterator;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

    private final PeekingIterator<RecordId> peekingIterator;
    private final RecordApi api;

    public GroupByRecordNameAndTypeIterator(RecordApi api, Iterator<RecordId> sortedIterator) {
        this.api = api;
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    @Override
    public ResourceRecordSet<?> next() {
        Record<?> record = api.get(peekingIterator.next());
        // it is possible that the record was deleted between the list and the get
        if (record == null)
            return null;
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.getFQDN())
                                                                .type(record.getType())
                                                                .ttl(record.getTTL())
                                                                .add(record.getRData());
        while (hasNext()) {
            Record<? extends Map<String, Object>> next = api.get(peekingIterator.peek());
            if (next == null)
                continue;
            if (fqdnAndTypeEquals(next, record)) {
                peekingIterator.next();
                builder.add(next.getRData());
            } else {
                break;
            }
        }
        return builder.build();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private static boolean fqdnAndTypeEquals(RecordId actual, RecordId expected) {
        return actual.getFQDN().equals(expected.getFQDN()) && actual.getType().equals(expected.getType());
    }
}