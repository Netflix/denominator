package denominator.ultradns;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jclouds.ultradns.ws.ResourceTypeToValue;
import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordMetadata;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.PeekingIterator;
import com.google.common.primitives.UnsignedInteger;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;

class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

    private final PeekingIterator<ResourceRecordMetadata> peekingIterator;
    private final static BiMap<UnsignedInteger, String> valueToType = new ResourceTypeToValue().inverse();

    public GroupByRecordNameAndTypeIterator(Iterator<ResourceRecordMetadata> sortedIterator) {
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    public boolean hasNext() {
        return peekingIterator.hasNext();
    }

    @Override
    public ResourceRecordSet<?> next() {
        ResourceRecord record = peekingIterator.next().getRecord();
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.getName())
                                                                .type(valueToType.get(record.getType()))
                                                                .ttl(record.getTTL());

        builder.add(parseRdataList(valueToType.get(record.getType()), record.getRData()));

        while (hasNext()) {
            ResourceRecord next = peekingIterator.peek().getRecord();
            if (fqdnAndTypeEquals(next, record)) {
                peekingIterator.next();
                builder.add(parseRdataList(valueToType.get(next.getType()), next.getRData()));
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
        return actual.getName().equals(expected.getName()) && actual.getType().equals(expected.getType());
    }

    static Map<String, Object> parseRdataList(String type, List<String> parts) {
        if ("A".equals(type)) {
            return AData.create(parts.get(0));
        } else if ("AAAA".equals(type)) {
            return AAAAData.create(parts.get(0));
        } else if ("CNAME".equals(type)) {
            return CNAMEData.create(parts.get(0));
        } else if ("MX".equals(type)) {
            return MXData.create(UnsignedInteger.valueOf(parts.get(0)), parts.get(1));
        } else if ("NS".equals(type)) {
            return NSData.create(parts.get(0));
        } else if ("PTR".equals(type)) {
            return PTRData.create(parts.get(0));
        } else if ("SOA".equals(type)) {
            return SOAData.builder()
                          .mname(parts.get(0))
                          .rname(parts.get(1))
                          .serial(UnsignedInteger.valueOf(parts.get(2)))
                          .refresh(UnsignedInteger.valueOf(parts.get(3)))
                          .retry(UnsignedInteger.valueOf(parts.get(4)))
                          .expire(UnsignedInteger.valueOf(parts.get(5)))
                          .minimum(UnsignedInteger.valueOf(parts.get(6))).build();
        } else if ("SPF".equals(type)) {
            return SPFData.create(parts.get(0));
        } else if ("SRV".equals(type)) {
            return SRVData.builder()
                          .priority(UnsignedInteger.valueOf(parts.get(0)))
                          .weight(UnsignedInteger.valueOf(parts.get(1)))
                          .port(UnsignedInteger.valueOf(parts.get(2)))
                          .target(parts.get(3)).build();
        } else if ("TXT".equals(type)) {
            return TXTData.create(parts.get(0));
        } else {
            return ImmutableMap.<String, Object> of("rdata", Joiner.on(' ').join(parts));
        }
    }
}