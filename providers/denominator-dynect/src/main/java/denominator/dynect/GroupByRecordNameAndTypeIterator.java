package denominator.dynect;
import static com.google.common.collect.Iterators.peekingIterator;
import static denominator.dynect.DynECTResourceRecordSetApi.numbersToInts;

import java.util.Iterator;
import java.util.Map;

import org.jclouds.dynect.v3.domain.Record;

import com.google.common.collect.PeekingIterator;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

    private final PeekingIterator<Record<?>> peekingIterator;

    public GroupByRecordNameAndTypeIterator(Iterator<Record<?>> sortedIterator) {
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    @Override
    public ResourceRecordSet<?> next() {
        Record<?> record = peekingIterator.next();

        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.getFQDN())
                                                                .type(record.getType())
                                                                .ttl(record.getTTL())
                                                                .add(numbersToInts(record));
        while (hasNext()) {
            Record<?> next = peekingIterator.peek();
            if (next == null)
                continue;
            if (fqdnAndTypeEquals(next, record)) {
                builder.add(peekingIterator.next().getRData());
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

    private static boolean fqdnAndTypeEquals(Record<?> actual, Record<?> expected) {
        return actual.getFQDN().equals(expected.getFQDN()) && actual.getType().equals(expected.getType());
    }
}