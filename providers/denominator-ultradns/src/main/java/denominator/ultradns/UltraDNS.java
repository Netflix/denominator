package denominator.ultradns;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import denominator.model.Zone;
import feign.RequestTemplate.Body;

interface UltraDNS {
    @POST
    @Body("<v01:getAccountsListOfUser/>")
    String accountId();

    @POST
    @Body("<v01:getZonesOfAccount><accountId>{accountId}</accountId><zoneType>all</zoneType></v01:getZonesOfAccount>")
    List<Zone> zonesOfAccount(@FormParam("accountId") String accountId);

    @POST
    @Body("<v01:getResourceRecordsOfZone><zoneName>{zoneName}</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone>")
    List<Record> recordsInZone(@FormParam("zoneName") String zoneName);

    @POST
    @Body("<v01:getResourceRecordsOfDNameByType><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><rrType>{rrType}</rrType></v01:getResourceRecordsOfDNameByType>")
    List<Record> recordsInZoneByNameAndType(@FormParam("zoneName") String zoneName,
            @FormParam("hostName") String hostName, @FormParam("rrType") int rrType);

    @POST
    void createRecordInZone(@FormParam("resourceRecord") Record create, @FormParam("zoneName") String zoneName);

    @POST
    void updateRecordInZone(@FormParam("resourceRecord") Record update, @FormParam("zoneName") String zoneName);

    @POST
    @Body("<v01:deleteResourceRecord><transactionID /><guid>{guid}</guid></v01:deleteResourceRecord>")
    void deleteRecord(@FormParam("guid") String guid);

    static class Record {
        String id;
        Date created;
        String name;
        int typeCode;
        int ttl;
        List<String> rdata = Lists.newArrayList();
    }

    @POST
    @Body("<v01:getLoadBalancingPoolsByZone><zoneName>{zoneName}</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone>")
    Table<String, String, String> rrPoolNameTypeToIdInZone(@FormParam("zoneName") String zoneName);

    @POST
    @Body("<v01:getRRPoolRecords><lbPoolId>{poolId}</lbPoolId></v01:getRRPoolRecords>")
    List<Record> recordsInRRPool(@FormParam("poolId") String poolId);

    @POST
    @Body("<v01:addRRLBPool><transactionID /><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><description>{poolRecordType}</description><poolRecordType>{poolRecordType}</poolRecordType><rrGUID /></v01:addRRLBPool>")
    String createRRPoolInZoneForNameAndType(@FormParam("zoneName") String zoneName, @FormParam("hostName") String name,
            @FormParam("poolRecordType") int typeCode);

    @POST
    @Body("<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"{lbPoolID}\" info1Value=\"{address}\" ZoneName=\"{zoneName}\" Type=\"{type}\" TTL=\"{ttl}\"/></v01:addRecordToRRPool>")
    void createRecordInRRPoolInZone(@FormParam("type") int type, @FormParam("ttl") int ttl,
            @FormParam("address") String rdata, @FormParam("lbPoolID") String lbPoolID,
            @FormParam("zoneName") String zoneName);

    @POST
    @Body("<v01:deleteLBPool><transactionID /><lbPoolID>{lbPoolID}</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool>")
    void deleteRRPool(@FormParam("lbPoolID") String id);

    @POST
    @Body("<v01:getAvailableRegions />")
    Table<String, Integer, SortedSet<String>> getRegionsByIdAndName();

    @POST
    @Body("<v01:getDirectionalDNSGroupDetails><GroupId>{GroupId}</GroupId></v01:getDirectionalDNSGroupDetails>")
    DirectionalGroup getDirectionalGroup(@FormParam("GroupId") String groupId);

    @POST
    @Body("<v01:getDirectionalDNSRecordsForGroup><groupName>{groupName}</groupName><hostName>{hostName}</hostName><zoneName>{zoneName}</zoneName><poolRecordType>{poolRecordType}</poolRecordType></v01:getDirectionalDNSRecordsForGroup>")
    List<DirectionalRecord> directionalRecordsInZoneAndGroupByNameAndType(@FormParam("zoneName") String zoneName,
            @FormParam("groupName") String groupName, @FormParam("hostName") String name,
            @FormParam("poolRecordType") int type);

    @POST
    String createRecordAndDirectionalGroupInPool(@FormParam("record") DirectionalRecord toCreate,
            @FormParam("group") DirectionalGroup group, @FormParam("poolId") String poolId);

    @POST
    void updateRecordAndDirectionalGroup(@FormParam("record") DirectionalRecord update,
            @FormParam("group") DirectionalGroup group);

    @POST
    @Body("<v01:getDirectionalPoolsOfZone><zoneName>{zoneName}</zoneName></v01:getDirectionalPoolsOfZone>")
    Map<String, String> directionalPoolNameToIdsInZone(@FormParam("zoneName") String zoneName);

    @POST
    @Body("<v01:getDirectionalDNSRecordsForHost><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><poolRecordType>{poolRecordType}</poolRecordType></v01:getDirectionalDNSRecordsForHost>")
    List<DirectionalRecord> directionalRecordsInZoneByNameAndType(@FormParam("zoneName") String zoneName,
            @FormParam("hostName") String name, @FormParam("poolRecordType") int rrType);

    @POST
    @Body("<v01:addDirectionalPool><transactionID /><AddDirectionalPoolData dirPoolType=\"GEOLOCATION\" poolRecordType=\"{poolRecordType}\" zoneName=\"{zoneName}\" hostName=\"{hostName}\" description=\"{poolRecordType}\"/></v01:addDirectionalPool>")
    String createDirectionalPoolInZoneForNameAndType(@FormParam("zoneName") String zoneName,
            @FormParam("hostName") String name, @FormParam("poolRecordType") String type);

    @POST
    @Body("<v01:deleteDirectionalPoolRecord><transactionID /><dirPoolRecordId>{dirPoolRecordId}</dirPoolRecordId></v01:deleteDirectionalPoolRecord>")
    void deleteDirectionalRecord(@FormParam("dirPoolRecordId") String id);

    @POST
    @Body("<v01:deleteDirectionalPool><transactionID /><dirPoolID>{dirPoolID}</dirPoolID><retainRecordID /></v01:deleteDirectionalPool>")
    void deleteDirectionalPool(@FormParam("dirPoolID") String dirPoolID);

    static class DirectionalGroup {
        String name;
        Multimap<String, String> regionToTerritories = LinkedListMultimap.create();
    }

    static class DirectionalRecord extends Record {
        String geoGroupId;
        String geoGroupName;
        boolean noResponseRecord;

        String type;
    }
}
