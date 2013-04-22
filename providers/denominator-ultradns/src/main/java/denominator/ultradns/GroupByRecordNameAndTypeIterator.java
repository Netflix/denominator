package denominator.ultradns;

import static com.google.common.collect.Iterators.peekingIterator;
import static denominator.ultradns.UltraDNSFunctions.toRdataMap;

import java.util.Iterator;
import java.util.Map;

import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail;

import com.google.common.collect.PeekingIterator;

import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {
    private final PeekingIterator<ResourceRecordDetail> peekingIterator;

    public GroupByRecordNameAndTypeIterator(Iterator<ResourceRecordDetail> sortedIterator) {
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    @Override
    public ResourceRecordSet<?> next() {
        ResourceRecord record = peekingIterator.next().getRecord();
        String type = new ResourceTypeToValue().inverse().get(record.getType());
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.getName())
                                                                .type(type)
                                                                .ttl(record.getTTL());

        builder.add(toRdataMap().apply(record));

        while (hasNext()) {
            ResourceRecord next = peekingIterator.peek().getRecord();
            if (fqdnAndTypeEquals(next, record)) {
                peekingIterator.next();
                builder.add(toRdataMap().apply(next));
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

    static boolean fqdnAndTypeEquals(ResourceRecord actual, ResourceRecord expected) {
        return actual.getName().equals(expected.getName()) && actual.getType() == expected.getType();
    }   
}