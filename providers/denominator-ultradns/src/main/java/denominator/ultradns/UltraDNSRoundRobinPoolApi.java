package denominator.ultradns;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;

import org.jclouds.ultradns.ws.domain.ResourceRecordMetadata;
import org.jclouds.ultradns.ws.domain.RoundRobinPool;
import org.jclouds.ultradns.ws.features.RoundRobinPoolApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedInteger;

class UltraDNSRoundRobinPoolApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UltraDNSRoundRobinPoolApi.class);
    private final RoundRobinPoolApi roundRobinPoolApi;

    
    public UltraDNSRoundRobinPoolApi(RoundRobinPoolApi roundRobinPoolApi) {
        this.roundRobinPoolApi = roundRobinPoolApi;
    }

    boolean isPoolType(String type) {
        return type.equals("A") || type.equals("AAAA");
    }
    
    
    public FluentIterable<ResourceRecordMetadata> listRoundRobinRecords() {
        ImmutableList.Builder<FluentIterable<ResourceRecordMetadata>> listBuilder = 
                ImmutableList.<FluentIterable<ResourceRecordMetadata>>builder();
        
        FluentIterable<RoundRobinPool> pools = roundRobinPoolApi.list();
        for (RoundRobinPool pool : pools) {
            listBuilder.add(roundRobinPoolApi.listRecords(pool.getId()));
        }
        
        return FluentIterable.from(Iterables.concat(listBuilder.build()));
    }


    public void add(String name, String type, UnsignedInteger ttl, List<Map<String, Object>> rdatas) {
        checkState(isPoolType(type), "not A or AAAA type");

        Optional<RoundRobinPool> pool = findPool(name, type);
        
        String poolId;
        if (!pool.isPresent()) {
            LOGGER.debug("No pool ({}) for type ({}) found", name, type);
            // see findPool for information on why we are storing the type in description field.
            if (type.equals("A")) {
                poolId = roundRobinPoolApi.createAPoolForHostname(type, name);
            } else { // or AAAA
                poolId = roundRobinPoolApi.createAAAAPoolForHostname(type, name);
            }
        } else {
            poolId = pool.get().getId();
        }
        
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

    static class GuidMatchPredicate implements Predicate<ResourceRecordMetadata> {
        private final String guid;
        public GuidMatchPredicate(String guid) {
            this.guid = guid;
        }
        
        @Override
        public boolean apply(ResourceRecordMetadata input) {
            return input.getGuid().equals(guid);
        }
    }
    
    /**
     * Remove a record from the pool and potentially the pool if there are no records left.
     * @param name the name of the pool
     * @param type the record type of the pool, A or AAAA
     * @param guid the guid of the record in the pool
     */
    public void remove(String name, String type, String guid) {
        for (RoundRobinPool pool : filterPools(name)) {
            FluentIterable<ResourceRecordMetadata> records = roundRobinPoolApi.listRecords(pool.getId());
            GuidMatchPredicate guidMatch = new GuidMatchPredicate(guid);

            if (!Iterables.isEmpty(records.filter(guidMatch))) {
                roundRobinPoolApi.deleteRecord(guid);
                if (records.filter(Predicates.not(guidMatch)).isEmpty()) {
                    roundRobinPoolApi.delete(pool.getId());
                }
            }
        }
    }
    
    protected FluentIterable<RoundRobinPool> filterPools(final String name) {
        return roundRobinPoolApi.list().filter(new Predicate<RoundRobinPool>() {
            @Override
            public boolean apply(RoundRobinPool pool) {
                return pool.getDName().equalsIgnoreCase(name); 
            }
        });
    }

    protected Optional<RoundRobinPool> findPool(final String name, final String type) {
        return roundRobinPoolApi.list().filter(new Predicate<RoundRobinPool>() {
            @Override
            public boolean apply(RoundRobinPool pool) {
                // TODO: unfortunately there is lack of information in the api to 
                // let us know the record type stored in the pool, so we cheat
                // and store the record type in the name aka description of the pool.
                // As soon as this is added to the vendor api we will remove this kludge.
                return pool.getDName().equalsIgnoreCase(name) && pool.getName().equals(type); 
            }
        }).first(); // there can be only 1 for a particular name and type.
    }
}
