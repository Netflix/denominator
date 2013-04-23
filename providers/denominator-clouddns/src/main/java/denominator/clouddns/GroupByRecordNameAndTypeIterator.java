package denominator.clouddns;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.Iterator;
import java.util.Map;

import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.jclouds.rackspace.clouddns.v1.domain.RecordDetail;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.PeekingIterator;

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

    private final PeekingIterator<RecordDetail> peekingIterator;

    public GroupByRecordNameAndTypeIterator(Iterator<RecordDetail> sortedIterator) {
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    @Override
    public ResourceRecordSet<?> next() {
        RecordDetail recordDetail = peekingIterator.next();
        // it is possible that the record was deleted between the list and the get
        if (recordDetail == null)
            return null;

        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(recordDetail.getName())
                                                                .type(recordDetail.getType())
                                                                .ttl(recordDetail.getTTL())
                                                                .add(toRData(recordDetail.getRecord()));
        while (hasNext()) {
            RecordDetail next = peekingIterator.peek();
            if (next == null)
                continue;
            if (nameAndTypeEquals(next, recordDetail)) {
                builder.add(toRData(peekingIterator.next().getRecord()));
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

    private static boolean nameAndTypeEquals(RecordDetail actual, RecordDetail expected) {
        return actual.getName().equals(expected.getName()) && actual.getType().equals(expected.getType());
    }

    static Map<String, Object> toRData(Record record) {
        if ("A".equals(record.getType())) {
            return AData.create(record.getData());
        } else if ("AAAA".equals(record.getType())) {
            return AAAAData.create(record.getData());
        } else if ("CNAME".equals(record.getType())) {
            return CNAMEData.create(record.getData());
        } else if ("MX".equals(record.getType())) {
            return MXData.create(record.getPriority(), record.getData());
        } else if ("NS".equals(record.getType())) {
            return NSData.create(record.getData());
        } else if ("PTR".equals(record.getType())) {
            return PTRData.create(record.getData());
        } else if ("SRV".equals(record.getType())) {
            ImmutableList<String> parts = split(record.getData());
            
            return SRVData.builder()
                          .priority(record.getPriority())
                          .weight(Integer.valueOf(parts.get(0)))
                          .port(Integer.valueOf(parts.get(1)))
                          .target(parts.get(2)).build();
        } else if ("TXT".equals(record.getType())) {
            return TXTData.create(record.getData());
        } else {
            return ImmutableMap.<String, Object> of("rdata", record.getData());
        }
    }

    private static ImmutableList<String> split(String rdata) {
        return ImmutableList.copyOf(Splitter.on(' ').split(rdata));
    }
}
