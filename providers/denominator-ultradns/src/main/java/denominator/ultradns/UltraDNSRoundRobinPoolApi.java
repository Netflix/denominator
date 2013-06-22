package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static denominator.ResourceTypeToValue.lookup;

import java.util.List;
import java.util.Map;

class UltraDNSRoundRobinPoolApi {
    private final UltraDNS api;
    private final String zoneName;

    UltraDNSRoundRobinPoolApi(UltraDNS api, String zoneName) {
        this.api = api;
        this.zoneName = zoneName;
    }

    boolean isPoolType(String type) {
        return type.equals("A") || type.equals("AAAA");
    }

    void add(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
        checkState(isPoolType(type), "%s not A or AAAA type", type);
        String poolId = reuseOrCreatePoolForNameAndType(name, type);
        for (Map<String, Object> rdata : rdatas) {
            String address = rdata.get("address").toString();
            int typeCode = lookup(type);
            api.createRecordInRRPoolInZone(typeCode, ttl, address, poolId, zoneName);
        }
    }

    private String reuseOrCreatePoolForNameAndType(String name, String type) {
        try {
            return api.createRRPoolInZoneForNameAndType(zoneName, name, lookup(type));
        } catch (UltraDNSException e) {
            // TODO: implement default fallback
            if (e.code() != UltraDNSException.POOL_ALREADY_EXISTS)
                throw e;
            return api.rrPoolNameTypeToIdInZone(zoneName).get(name, type);
        }
    }

    void deletePool(String name, String type) {
        checkNotNull(name, "pool name was null");
        checkNotNull(type, "pool record type was null");
        String poolId = api.rrPoolNameTypeToIdInZone(zoneName).get(name, type);
        if (poolId != null) {
            if (api.recordsInRRPool(poolId).isEmpty()) {
                try {
                    api.deleteRRPool(poolId);
                } catch (UltraDNSException e) {
                    // TODO: implement default fallback
                    if (e.code() != UltraDNSException.POOL_NOT_FOUND)
                        throw e;
                }
            }
        }
    }
}
