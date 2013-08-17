package denominator.ultradns;

import static denominator.common.Util.peekingIterator;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;

import java.util.Iterator;
import java.util.Map;

import denominator.ResourceTypeToValue;
import denominator.common.PeekingIterator;
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

    @Override
    public ResourceRecordSet<?> next() {
        Record record = peekingIterator.next();
        String type = ResourceTypeToValue.lookup(record.typeCode);
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.name)
                                                                .type(type)
                                                                .ttl(record.ttl);

        builder.add(forTypeAndRData(type, record.rdata));

        while (hasNext()) {
            Record next = peekingIterator.peek();
            if (fqdnAndTypeEquals(next, record)) {
                peekingIterator.next();
                builder.add(forTypeAndRData(type, next.rdata));
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
