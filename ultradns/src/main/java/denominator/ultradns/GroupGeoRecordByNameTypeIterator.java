package denominator.ultradns;

import static denominator.common.Util.peekingIterator;
import static denominator.ultradns.UltraDNSFunctions.forTypeAndRData;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import denominator.common.PeekingIterator;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Geo;
import denominator.ultradns.UltraDNS.DirectionalRecord;

/**
 * Generally, this iterator will produce {@link ResourceRecordSet} for only a
 * single record type. However, there are special cases where this can produce
 * multiple. For example, {@link DirectionalPool.RecordType#IPV4} and
 * {@link DirectionalPool.RecordType#IPV6} emit both address ({@code A} or
 * {@code AAAA}) and {@code CNAME} records.
 */
class GroupGeoRecordByNameTypeIterator implements Iterator<ResourceRecordSet<?>> {

    static final class Factory {
        private final UltraDNS api;

        @Inject
        Factory(UltraDNS api) {
            this.api = api;
        }

        /**
         * @param sortedIterator
         *            only contains records with the same
         *            {@link DirectionalRecord#name()}, sorted by
         *            {@link DirectionalRecord#type()},
         *            {@link DirectionalRecord#getGeolocationGroup()} or
         *            {@link DirectionalRecord#group()}
         */
        Iterator<ResourceRecordSet<?>> create(Iterator<DirectionalRecord> sortedIterator) {
            return new GroupGeoRecordByNameTypeIterator(api, sortedIterator);
        }
    }
    private final Map<String, Geo> cache = new LinkedHashMap<String, Geo>();

    private final UltraDNS api;
    private final PeekingIterator<DirectionalRecord> peekingIterator;

    private GroupGeoRecordByNameTypeIterator(UltraDNS api, Iterator<DirectionalRecord> sortedIterator) {
        this.api = api;
        this.peekingIterator = peekingIterator(sortedIterator);
    }

    /**
     * skips no response records as they aren't portable
     */
    @Override
    public boolean hasNext() {
        if (!peekingIterator.hasNext())
            return false;
        DirectionalRecord record = peekingIterator.peek();
        if (record.noResponseRecord) {
            // TODO: log as this is unsupported
            peekingIterator.next();
        }
        return true;
    }

    @Override
    public ResourceRecordSet<?> next() {
        DirectionalRecord record = peekingIterator.next();

        Builder<Map<String, Object>> builder = ResourceRecordSet.builder().name(record.name).type(record.type)
                .qualifier(record.geoGroupName).ttl(record.ttl);

        builder.add(forTypeAndRData(record.type, record.rdata));
        
        if (!cache.containsKey(record.geoGroupId)) {
            Geo profile = Geo.create(api.getDirectionalDNSGroupDetails(record.geoGroupId).regionToTerritories);
            cache.put(record.geoGroupId,profile);
        }

        builder.addProfile(cache.get(record.geoGroupId));
        while (hasNext()) {
            DirectionalRecord next = peekingIterator.peek();
            if (typeTTLAndGeoGroupEquals(next, record)) {
                peekingIterator.next();
                builder.add(forTypeAndRData(record.type, next.rdata));
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

    static boolean typeTTLAndGeoGroupEquals(DirectionalRecord actual, DirectionalRecord expected) {
        return actual.type.equals(expected.type) && actual.ttl == expected.ttl
                && actual.geoGroupId.equals(expected.geoGroupId);
    }
}
