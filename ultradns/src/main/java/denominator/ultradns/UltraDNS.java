package denominator.ultradns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Named;

import denominator.model.Zone;
import feign.Body;
import feign.RequestLine;

interface UltraDNS {

    @RequestLine("POST")
    @Body("<v01:getNeustarNetworkStatus/>")
    NetworkStatus networkStatus();

    static enum NetworkStatus {
        GOOD,
        FAILED;
    }

    @RequestLine("POST")
    @Body("<v01:getAccountsListOfUser/>")
    String accountId();

    @RequestLine("POST")
    @Body("<v01:getZonesOfAccount><accountId>{accountId}</accountId><zoneType>all</zoneType></v01:getZonesOfAccount>")
    List<Zone> zonesOfAccount(@Named("accountId") String accountId);

    @RequestLine("POST")
    @Body("<v01:getResourceRecordsOfZone><zoneName>{zoneName}</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone>")
    List<Record> recordsInZone(@Named("zoneName") String zoneName);

    @RequestLine("POST")
    @Body("<v01:getResourceRecordsOfDNameByType><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><rrType>{rrType}</rrType></v01:getResourceRecordsOfDNameByType>")
    List<Record> recordsInZoneByNameAndType(@Named("zoneName") String zoneName, @Named("hostName") String hostName,
            @Named("rrType") int rrType);

    @RequestLine("POST")
    void createRecordInZone(@Named("resourceRecord") Record create, @Named("zoneName") String zoneName);

    @RequestLine("POST")
    void updateRecordInZone(@Named("resourceRecord") Record update, @Named("zoneName") String zoneName);

    /**
     * @throws UltraDNSException with code {@link UltraDNSException#RESOURCE_RECORD_NOT_FOUND}.
     */
    @RequestLine("POST")
    @Body("<v01:deleteResourceRecord><transactionID /><guid>{guid}</guid></v01:deleteResourceRecord>")
    void deleteRecord(@Named("guid") String guid);

    static class Record {
        String id;
        Date created;
        String name;
        int typeCode;
        int ttl;
        List<String> rdata = new ArrayList<String>();
    }

    @RequestLine("POST")
    @Body("<v01:getLoadBalancingPoolsByZone><zoneName>{zoneName}</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone>")
    Map<NameAndType, String> rrPoolNameTypeToIdInZone(@Named("zoneName") String zoneName);

    static class NameAndType {
        String name;
        String type;

        @Override
        public int hashCode() {
            return 37 * name.hashCode() + type.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || !(obj instanceof NameAndType))
                return false;
            NameAndType that = NameAndType.class.cast(obj);
            return this.name.equals(that.name) && this.type.equals(that.type);
        }

        @Override
        public String toString() {
            return "NameAndType(" + name + "," + type + ")";
        }
    }

    @RequestLine("POST")
    @Body("<v01:getRRPoolRecords><lbPoolId>{poolId}</lbPoolId></v01:getRRPoolRecords>")
    List<Record> recordsInRRPool(@Named("poolId") String poolId);

    @RequestLine("POST")
    @Body("<v01:addRRLBPool><transactionID /><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><description>{poolRecordType}</description><poolRecordType>{poolRecordType}</poolRecordType><rrGUID /></v01:addRRLBPool>")
    String createRRPoolInZoneForNameAndType(@Named("zoneName") String zoneName, @Named("hostName") String name,
            @Named("poolRecordType") int typeCode);

    @RequestLine("POST")
    @Body("<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"{lbPoolID}\" info1Value=\"{address}\" ZoneName=\"{zoneName}\" Type=\"{type}\" TTL=\"{ttl}\"/></v01:addRecordToRRPool>")
    void createRecordInRRPoolInZone(@Named("type") int type, @Named("ttl") int ttl, @Named("address") String rdata,
            @Named("lbPoolID") String lbPoolID, @Named("zoneName") String zoneName);

    /**
     * @throws UltraDNSException with code {@link UltraDNSException#POOL_NOT_FOUND} and {@link UltraDNSException#RESOURCE_RECORD_NOT_FOUND}.
     */
    @RequestLine("POST")
    @Body("<v01:deleteLBPool><transactionID /><lbPoolID>{lbPoolID}</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool>")
    void deleteRRPool(@Named("lbPoolID") String id);

    @RequestLine("POST")
    @Body("<v01:getAvailableRegions />")
    Map<String, Collection<String>> availableRegions();

    @RequestLine("POST")
    @Body("<v01:getDirectionalDNSGroupDetails><GroupId>{GroupId}</GroupId></v01:getDirectionalDNSGroupDetails>")
    DirectionalGroup getDirectionalGroup(@Named("GroupId") String groupId);

    @RequestLine("POST")
    @Body("<v01:getDirectionalDNSRecordsForGroup><groupName>{groupName}</groupName><hostName>{hostName}</hostName><zoneName>{zoneName}</zoneName><poolRecordType>{poolRecordType}</poolRecordType></v01:getDirectionalDNSRecordsForGroup>")
    List<DirectionalRecord> directionalRecordsInZoneAndGroupByNameAndType(@Named("zoneName") String zoneName,
            @Named("groupName") String groupName, @Named("hostName") String name, @Named("poolRecordType") int type);

    /**
     * @throws UltraDNSException with code {@link UltraDNSException#POOL_RECORD_ALREADY_EXISTS}.
     */
    @RequestLine("POST")
    String createRecordAndDirectionalGroupInPool(@Named("record") DirectionalRecord toCreate,
            @Named("group") DirectionalGroup group, @Named("poolId") String poolId);

    /**
     * @throws UltraDNSException with code {@link UltraDNSException#RESOURCE_RECORD_ALREADY_EXISTS}.
     */
    @RequestLine("POST")
    void updateRecordAndDirectionalGroup(@Named("record") DirectionalRecord update,
            @Named("group") DirectionalGroup group);

    @RequestLine("POST")
    @Body("<v01:getDirectionalPoolsOfZone><zoneName>{zoneName}</zoneName></v01:getDirectionalPoolsOfZone>")
    Map<String, String> directionalPoolNameToIdsInZone(@Named("zoneName") String zoneName);

    @RequestLine("POST")
    @Body("<v01:getDirectionalDNSRecordsForHost><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><poolRecordType>{poolRecordType}</poolRecordType></v01:getDirectionalDNSRecordsForHost>")
    List<DirectionalRecord> directionalRecordsInZoneByNameAndType(@Named("zoneName") String zoneName,
            @Named("hostName") String name, @Named("poolRecordType") int rrType);

    @RequestLine("POST")
    @Body("<v01:addDirectionalPool><transactionID /><AddDirectionalPoolData dirPoolType=\"GEOLOCATION\" poolRecordType=\"{poolRecordType}\" zoneName=\"{zoneName}\" hostName=\"{hostName}\" description=\"{poolRecordType}\"/></v01:addDirectionalPool>")
    String createDirectionalPoolInZoneForNameAndType(@Named("zoneName") String zoneName,
            @Named("hostName") String name, @Named("poolRecordType") String type);

    @RequestLine("POST")
    @Body("<v01:deleteDirectionalPoolRecord><transactionID /><dirPoolRecordId>{dirPoolRecordId}</dirPoolRecordId></v01:deleteDirectionalPoolRecord>")
    void deleteDirectionalRecord(@Named("dirPoolRecordId") String id);

    @RequestLine("POST")
    @Body("<v01:deleteDirectionalPool><transactionID /><dirPoolID>{dirPoolID}</dirPoolID><retainRecordID /></v01:deleteDirectionalPool>")
    void deleteDirectionalPool(@Named("dirPoolID") String dirPoolID);

    static class DirectionalGroup {
        String name;
        Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();
    }

    static class DirectionalRecord extends Record {
        String geoGroupId;
        String geoGroupName;
        boolean noResponseRecord;

        String type;
    }
}
