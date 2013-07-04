package denominator.clouddns;

import static denominator.common.Util.peekingIterator;
import static denominator.common.Util.split;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.clouddns.RackspaceApis.Record;
import denominator.common.PeekingIterator;
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
            List<String> parts = split(' ', record.data);

            return SRVData.builder()
                          .priority(record.priority)
                          .weight(Integer.valueOf(parts.get(0)))
                          .port(Integer.valueOf(parts.get(1)))
                          .target(parts.get(2)).build();
        } else if ("TXT".equals(record.type)) {
            return TXTData.create(record.data);
        } else {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("rdata", record.data);
            return map;
        }
    }
}
