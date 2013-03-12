package denominator.dynect;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.Iterator;
import java.util.Map;

import org.jclouds.dynect.v3.domain.Record;
import org.jclouds.dynect.v3.domain.RecordId;
import org.jclouds.dynect.v3.features.RecordApi;

import com.google.common.collect.ImmutableMap;
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
        Record<?> record = getRecord(api, peekingIterator.next());
        // it is possible that the record was deleted between the list and the get
        if (record == null)
            return null;
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(record.getFQDN())
                                                                .type(record.getType())
                                                                .ttl(record.getTTL().intValue())
                                                                .add(record.getRData());
        while (hasNext()) {
            RecordId next = peekingIterator.peek();
            if (next == null)
                continue;
            if (fqdnAndTypeEquals(next, record)) {
                builder.add(getRecord(api, peekingIterator.next()).getRData());
            } else {
                break;
            }
        }
        return builder.build();
    }

    static Record<? extends Map<String, Object>> getRecord(RecordApi api, RecordId recordId) {
        if ("A".equals(recordId.getType())) {
            return api.getA(recordId.getFQDN(), recordId.getId());
        } else if ("AAAA".equals(recordId.getType())) {
            return api.getAAAA(recordId.getFQDN(), recordId.getId());
        } else if ("CNAME".equals(recordId.getType())) {
            return api.getCNAME(recordId.getFQDN(), recordId.getId());
        } else if ("MX".equals(recordId.getType())) {
            return api.getMX(recordId.getFQDN(), recordId.getId());
        } else if ("NS".equals(recordId.getType())) {
            return api.getNS(recordId.getFQDN(), recordId.getId());
        } else if ("PTR".equals(recordId.getType())) {
            return api.getPTR(recordId.getFQDN(), recordId.getId());
        } else if ("SOA".equals(recordId.getType())) {
            return api.getSOA(recordId.getFQDN(), recordId.getId());
        } else if ("SRV".equals(recordId.getType())) {
            return api.getSRV(recordId.getFQDN(), recordId.getId());
        } else if ("SSHFP".equals(recordId.getType())) {
            Record<? extends Map<String, Object>> sshFP = api.get(recordId);
            if (sshFP == null)
                return null;
            // temporary until jclouds 1.6 rc2, as ints are coming out as doubles.
            Map<String, Object> rdata = ImmutableMap.<String, Object> builder()
                    .put("algorithm", Integer.valueOf(Double.class.cast(sshFP.getRData().get("algorithm")).intValue()))
                    .put("fptype", Integer.valueOf(Double.class.cast(sshFP.getRData().get("fptype")).intValue()))
                    .put("fingerprint", sshFP.getRData().get("fingerprint")).build();
            return Record.builder().from(sshFP).ttl(sshFP.getTTL()).rdata(rdata).build();
        } else if ("TXT".equals(recordId.getType())) {
            return api.getTXT(recordId.getFQDN(), recordId.getId());
        } else {
            return api.get(recordId);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private static boolean fqdnAndTypeEquals(RecordId actual, RecordId expected) {
        return actual.getFQDN().equals(expected.getFQDN()) && actual.getType().equals(expected.getType());
    }
}