package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.compose;
import static denominator.ultradns.UltraDNSFunctions.toResourceRecord;
import static denominator.ultradns.UltraDNSPredicates.recordGuidEqualTo;
import static denominator.ultradns.UltraDNSPredicates.resourceTypeEqualTo;

import java.util.List;
import java.util.Map;

import org.jclouds.ultradns.ws.domain.ResourceRecordMetadata;
import org.jclouds.ultradns.ws.domain.RoundRobinPool;
import org.jclouds.ultradns.ws.features.RoundRobinPoolApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.primitives.UnsignedInteger;

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

    void add(String name, String type, UnsignedInteger ttl, List<Map<String, Object>> rdatas) {
        checkState(isPoolType(type), "not A or AAAA type");

        String poolId = reuseOrCreatePoolForNameAndType(name, type);

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

    private String reuseOrCreatePoolForNameAndType(String name, String type) {
        Optional<RoundRobinPool> pool = filterPoolsByNameAndType(name, type).first();
        if (!pool.isPresent()) {
            LOGGER.debug("No pool ({}) for type ({}) found", name, type);
            // see findPool for information on why we are storing the type in
            // description field.
            if (type.equals("A")) {
                return roundRobinPoolApi.createAPoolForHostname(type, name);
            } else { // or AAAA
                return roundRobinPoolApi.createAAAAPoolForHostname(type, name);
            }
        } else {
            return pool.get().getId();
        }
    }

    /**
     * Remove a record from the pool and potentially the pool if there are no
     * records left.
     * 
     * @param name
     *            the name of the pool
     * @param type
     *            the record type of the pool, A or AAAA
     * @param guid
     *            the guid of the record in the pool
     */
    void remove(String name, String type, String guid) {
        for (RoundRobinPool pool : filterPoolsByNameAndPredicate(name, recordGuidEqualTo(guid))) {
            roundRobinPoolApi.deleteRecord(guid);
            if (roundRobinPoolApi.listRecords(pool.getId()).isEmpty()) {
                roundRobinPoolApi.delete(pool.getId());
            }
        }
    }

    private FluentIterable<RoundRobinPool> filterPoolsByNameAndType(final String name, String type) {
        checkNotNull(name, "pool name was null");
        checkNotNull(type, "resource type was null");
        Predicate<ResourceRecordMetadata> recordPredicate = compose(
                resourceTypeEqualTo(new ResourceTypeToValue().get(type)), toResourceRecord());
        return filterPoolsByNameAndPredicate(name, recordPredicate);
    }

    private FluentIterable<RoundRobinPool> filterPoolsByNameAndPredicate(final String name,
            final Predicate<ResourceRecordMetadata> recordPredicate) {
        return roundRobinPoolApi.list().filter(new Predicate<RoundRobinPool>() {
            @Override
            public boolean apply(RoundRobinPool pool) {
                if (!pool.getDName().equalsIgnoreCase(name))
                    return false;
                return roundRobinPoolApi.listRecords(pool.getId()).anyMatch(recordPredicate);
            }
        });
    }
}
