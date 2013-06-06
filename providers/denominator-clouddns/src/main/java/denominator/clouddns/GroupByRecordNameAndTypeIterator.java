package denominator.clouddns;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.PeekingIterator;

import denominator.clouddns.RackspaceApis.Record;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;

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
        Record recordDetail = peekingIterator.next();
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(recordDetail.name)
                                                                .type(recordDetail.type)
                                                                .ttl(recordDetail.ttl)
                                                                .add(toRData(recordDetail));
        while (hasNext()) {
            Record next = peekingIterator.peek();
            if (next == null)
                continue;
            if (nameAndTypeEquals(next, recordDetail)) {
                builder.add(toRData(peekingIterator.next()));
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

    private static boolean nameAndTypeEquals(Record actual, Record expected) {
        return actual.name.equals(expected.name) && actual.type.equals(expected.type);
    }

    static Map<String, Object> toRData(Record record) {
        if ("A".equals(record.type)) {
            return AData.create(record.data);
        } else if ("AAAA".equals(record.type)) {
            return AAAAData.create(record.data);
        } else if ("CNAME".equals(record.type)) {
            return CNAMEData.create(record.data);
        } else if ("MX".equals(record.type)) {
            return MXData.create(record.priority, record.data);
        } else if ("NS".equals(record.type)) {
            return NSData.create(record.data);
        } else if ("PTR".equals(record.type)) {
            return PTRData.create(record.data);
        } else if ("SRV".equals(record.type)) {
            ImmutableList<String> parts = split(record.data);
            
            return SRVData.builder()
                          .priority(record.priority)
                          .weight(Integer.valueOf(parts.get(0)))
                          .port(Integer.valueOf(parts.get(1)))
                          .target(parts.get(2)).build();
        } else if ("TXT".equals(record.type)) {
            return TXTData.create(record.data);
        } else {
            return ImmutableMap.<String, Object> of("rdata", record.data);
        }
    }

    private static ImmutableList<String> split(String rdata) {
        return ImmutableList.copyOf(Splitter.on(' ').split(rdata));
    }
}
