package denominator.ultradns;

import static com.google.common.collect.Iterators.peekingIterator;
import static denominator.ultradns.UltraDNSFunctions.toRdataMap;

import java.util.Iterator;
import java.util.Map;


import com.google.common.collect.BiMap;
import com.google.common.collect.PeekingIterator;

import denominator.ResourceTypeToValue;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.ultradns.UltraDNS.Record;

class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {
    private final PeekingIterator<Record> peekingIterator;

    public GroupByRecordNameAndTypeIterator(Iterator<Record> sortedIterator) {
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    private static final BiMap<Integer, String> reverseLookup = new ResourceTypeToValue().inverse();

    @Override
    public ResourceRecordSet<?> next() {
        Record record = peekingIterator.next();
        String type = reverseLookup.get(record.typeCode);
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.name)
                                                                .type(type)
                                                                .ttl(record.ttl);

        builder.add(toRdataMap().apply(record));

        while (hasNext()) {
            Record next = peekingIterator.peek();
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

    static boolean fqdnAndTypeEquals(Record actual, Record expected) {
        return actual.name.equals(expected.name) && actual.typeCode == expected.typeCode;
    }   
}
