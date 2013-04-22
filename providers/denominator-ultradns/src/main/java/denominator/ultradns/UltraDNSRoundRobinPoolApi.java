package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.compose;
import static denominator.ultradns.UltraDNSFunctions.toResourceRecord;
import static denominator.ultradns.UltraDNSPredicates.poolDNameEqualTo;
import static denominator.ultradns.UltraDNSPredicates.poolNameEqualTo;
import static denominator.ultradns.UltraDNSPredicates.recordGuidEqualTo;
import static denominator.ultradns.UltraDNSPredicates.resourceTypeEqualTo;
import static org.jclouds.ultradns.ws.domain.RoundRobinPool.RecordType.A;
import static org.jclouds.ultradns.ws.domain.RoundRobinPool.RecordType.AAAA;

import java.util.List;
import java.util.Map;

import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail;
import org.jclouds.ultradns.ws.domain.RoundRobinPool;
import org.jclouds.ultradns.ws.features.RoundRobinPoolApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import denominator.ResourceTypeToValue;
class UltraDNSRoundRobinPoolApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UltraDNSRoundRobinPoolApi.class);
    private final RoundRobinPoolApi roundRobinPoolApi;

    public UltraDNSRoundRobinPoolApi(RoundRobinPoolApi roundRobinPoolApi) {
        this.roundRobinPoolApi = roundRobinPoolApi;
    }

    boolean isPoolType(String type) {
        return type.equals("A") || type.equals("AAAA");
    }

    void add(String dname, String type, int ttl, List<Map<String, Object>> rdatas) {
        checkState(isPoolType(type), "not A or AAAA type");

        String poolId = reuseOrCreatePoolForNameAndType(dname, type);

        for (Map<String, Object> rdata : rdatas) {
            String recordId = null;
            String address = rdata.get("address").toString();
            if (type.equals("A")) {
                recordId = roundRobinPoolApi.addARecordWithAddressAndTTL(poolId, address, ttl);
            } else {
                recordId = roundRobinPoolApi.addAAAARecordWithAddressAndTTL(poolId, address, ttl);
            }
            LOGGER.debug("record ({}) created with id({})", address, recordId);
        }
    }

    private String reuseOrCreatePoolForNameAndType(String dname, String type) {
        Optional<RoundRobinPool> pool = firstPoolWithDNameAndType(dname, type);
        if (!pool.isPresent()) {
            LOGGER.debug("No pool ({}) for type ({}) found", dname, type);
            // see findPool for information on why we are storing the type in
            // description field.
            if (type.equals("A")) {
                return roundRobinPoolApi.createForDNameAndType(type, dname, A.getCode());
            } else { // or AAAA
                return roundRobinPoolApi.createForDNameAndType(type, dname, AAAA.getCode());
            }
        } else {
            return pool.get().getId();
        }
    }

    private Optional<RoundRobinPool> firstPoolWithDNameAndType(String dname, String type) {
        checkNotNull(dname, "pool dname was null");
        checkNotNull(type, "resource type was null");

        FluentIterable<RoundRobinPool> pools = roundRobinPoolApi.list().filter(poolDNameEqualTo(dname));
        
        // first, try a cheap match as it avoids having to search through all
        // records of a pool that happen to be ahead in line. This occurs when
        // you are adding to a AAAA pool, which is lexicographically after A.
        Predicate<RoundRobinPool> cheapPredicate = poolNameEqualTo(type);
        Optional<RoundRobinPool> match = pools.firstMatch(cheapPredicate);
        if (match.isPresent())
            return match;

        // failing above, we need to exhaustive search in order to find any pools
        // that may not follow our naming convention, but are present for the
        // correct dname.
        Predicate<ResourceRecord> resourceResourceDetailPredicate = resourceTypeEqualTo(new ResourceTypeToValue()
                .get(type));
        Predicate<RoundRobinPool> expensivePredicate = toRoundRobinPoolPredicate(compose(
                resourceResourceDetailPredicate, toResourceRecord()));
        return pools.firstMatch(expensivePredicate);
    }

    /**
     * Remove a record from the pool and potentially the pool if there are no
     * records left.
     * 
     * @param name
     *            the name of the pool
     * @param guid
     *            the guid of the record in the pool
     */
    void remove(String dname, String guid) {
        checkNotNull(dname, "pool dname was null");
        checkNotNull(guid, "record guid was null");
        Predicate<RoundRobinPool> dnameWithRecord = and(poolDNameEqualTo(dname),
                toRoundRobinPoolPredicate(recordGuidEqualTo(guid)));
        Optional<RoundRobinPool> poolContainingRecord = roundRobinPoolApi.list().firstMatch(dnameWithRecord);
        roundRobinPoolApi.deleteRecord(guid);
        if (poolContainingRecord.isPresent()) {
            if (roundRobinPoolApi.listRecords(poolContainingRecord.get().getId()).isEmpty()) {
                roundRobinPoolApi.delete(poolContainingRecord.get().getId());
            }
        }
    }

    private Predicate<RoundRobinPool> toRoundRobinPoolPredicate(
            final Predicate<ResourceRecordDetail> resourceResourceDetailPredicate) {
        return new Predicate<RoundRobinPool>() {
            @Override
            public boolean apply(RoundRobinPool pool) {
                return roundRobinPoolApi.listRecords(pool.getId()).anyMatch(resourceResourceDetailPredicate);
            }

            @Override
            public String toString() {
                return "AnyRecordMatches(" + resourceResourceDetailPredicate + ")";
            }
        };
    }
}
