package denominator.ultradns;

import java.util.List;
import java.util.Map;

import denominator.ultradns.UltraDNS.NameAndType;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Preconditions.checkState;

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
      api.addRecordToRRPool(typeCode, ttl, address, poolId, zoneName);
    }
  }

  private String reuseOrCreatePoolForNameAndType(String name, String type) {
    try {
      return api.addRRLBPool(zoneName, name, lookup(type));
    } catch (UltraDNSException e) {
      if (e.code() != UltraDNSException.POOL_ALREADY_EXISTS) {
        throw e;
      }
      return getPoolByNameAndType(name, type);
    }
  }

  String getPoolByNameAndType(String name, String type) {
    NameAndType nameAndType = new NameAndType();
    nameAndType.name = name;
    nameAndType.type = type;
    return api.getLoadBalancingPoolsByZone(zoneName).get(nameAndType);
  }

  void deletePool(String name, String type) {
    NameAndType nameAndType = new NameAndType();
    nameAndType.name = checkNotNull(name, "pool name was null");
    nameAndType.type = checkNotNull(type, "pool record type was null");
    String poolId = api.getLoadBalancingPoolsByZone(zoneName).get(nameAndType);
    if (poolId != null) {
      if (api.getRRPoolRecords(poolId).isEmpty()) {
        try {
          api.deleteLBPool(poolId);
        } catch (UltraDNSException e) {
          switch (e.code()) {
            // lost race
            case UltraDNSException.POOL_NOT_FOUND:
            case UltraDNSException.RESOURCE_RECORD_NOT_FOUND:
              return;
          }
          throw e;
        }
      }
    }
  }
}
