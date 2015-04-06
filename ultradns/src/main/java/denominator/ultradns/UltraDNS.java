package denominator.ultradns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@Headers("Content-Type: application/xml")
interface UltraDNS {

  @RequestLine("POST")
  @Body("<v01:getNeustarNetworkStatus/>")
  NetworkStatus getNeustarNetworkStatus();

  @RequestLine("POST")
  @Body("<v01:getAccountsListOfUser/>")
  String getAccountsListOfUser();

  @RequestLine("POST")
  @Body("<v01:getZonesOfAccount><accountId>{accountId}</accountId><zoneType>all</zoneType></v01:getZonesOfAccount>")
  List<String> getZonesOfAccount(@Param("accountId") String accountId);

  @RequestLine("POST")
  @Body("<v01:createPrimaryZone><transactionID/><accountId>{accountId}</accountId><zoneName>{zoneName}</zoneName><forceImport>false</forceImport></v01:createPrimaryZone>")
  void createPrimaryZone(@Param("accountId") String accountId, @Param("zoneName") String zoneName);

  /**
   * @throws UltraDNSException with code {@link UltraDNSException#ZONE_NOT_FOUND}.
   */
  @RequestLine("POST")
  @Body("<v01:deleteZone><transactionID /><zoneName>{zoneName}</zoneName></v01:deleteZone>")
  void deleteZone(@Param("zoneName") String zoneName);

  @RequestLine("POST")
  @Body("<v01:getResourceRecordsOfZone><zoneName>{zoneName}</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone>")
  List<Record> getResourceRecordsOfZone(@Param("zoneName") String zoneName);

  @RequestLine("POST")
  @Body("<v01:getResourceRecordsOfDNameByType><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><rrType>{rrType}</rrType></v01:getResourceRecordsOfDNameByType>")
  List<Record> getResourceRecordsOfDNameByType(@Param("zoneName") String zoneName,
                                               @Param("hostName") String hostName,
                                               @Param("rrType") int rrType);

  @RequestLine("POST")
  void createResourceRecord(@Param("resourceRecord") Record create,
                            @Param("zoneName") String zoneName);

  @RequestLine("POST")
  void updateResourceRecord(@Param("resourceRecord") Record update,
                            @Param("zoneName") String zoneName);

  /**
   * @throws UltraDNSException with code {@link UltraDNSException#RESOURCE_RECORD_NOT_FOUND} .
   */
  @RequestLine("POST")
  @Body("<v01:deleteResourceRecord><transactionID /><guid>{guid}</guid></v01:deleteResourceRecord>")
  void deleteResourceRecord(@Param("guid") String guid);

  @RequestLine("POST")
  @Body("<v01:getLoadBalancingPoolsByZone><zoneName>{zoneName}</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone>")
  Map<NameAndType, String> getLoadBalancingPoolsByZone(@Param("zoneName") String zoneName);

  @RequestLine("POST")
  @Body("<v01:getRRPoolRecords><lbPoolId>{poolId}</lbPoolId></v01:getRRPoolRecords>")
  List<Record> getRRPoolRecords(@Param("poolId") String poolId);

  @RequestLine("POST")
  @Body("<v01:addRRLBPool><transactionID /><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><description>{poolRecordType}</description><poolRecordType>{poolRecordType}</poolRecordType><rrGUID /></v01:addRRLBPool>")
  String addRRLBPool(@Param("zoneName") String zoneName, @Param("hostName") String name,
                     @Param("poolRecordType") int typeCode);

  @RequestLine("POST")
  @Body("<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"{lbPoolID}\" info1Value=\"{address}\" ZoneName=\"{zoneName}\" Type=\"{type}\" TTL=\"{ttl}\"/></v01:addRecordToRRPool>")
  void addRecordToRRPool(@Param("type") int type, @Param("ttl") int ttl,
                         @Param("address") String rdata,
                         @Param("lbPoolID") String lbPoolID, @Param("zoneName") String zoneName);

  @RequestLine("POST")
  @Body("<v01:updateRecordOfRRPool><transactionID /><resourceRecord rrGuid=\"{rrGuid}\" lbPoolID=\"{lbPoolID}\" info1Value=\"{info1Value}\" TTL=\"{ttl}\"/></v01:updateRecordOfRRPool>")
  void updateRecordOfRRPool(@Param("rrGuid") String rrGuid, @Param("lbPoolID") String lbPoolID,
                            @Param("info1Value") String info1Value, @Param("ttl") int ttl);

  /**
   * @throws UltraDNSException with code {@link UltraDNSException#POOL_NOT_FOUND} and {@link
   *                           UltraDNSException#RESOURCE_RECORD_NOT_FOUND}.
   */
  @RequestLine("POST")
  @Body("<v01:deleteLBPool><transactionID /><lbPoolID>{lbPoolID}</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool>")
  void deleteLBPool(@Param("lbPoolID") String id);

  @RequestLine("POST")
  @Body("<v01:getAvailableRegions />")
  Map<String, Collection<String>> getAvailableRegions();

  @RequestLine("POST")
  @Body("<v01:getDirectionalDNSGroupDetails><GroupId>{GroupId}</GroupId></v01:getDirectionalDNSGroupDetails>")
  DirectionalGroup getDirectionalDNSGroupDetails(@Param("GroupId") String groupId);

  @RequestLine("POST")
  @Body("<v01:getDirectionalDNSRecordsForGroup><groupName>{groupName}</groupName><hostName>{hostName}</hostName><zoneName>{zoneName}</zoneName><poolRecordType>{poolRecordType}</poolRecordType></v01:getDirectionalDNSRecordsForGroup>")
  List<DirectionalRecord> getDirectionalDNSRecordsForGroup(@Param("zoneName") String zoneName,
                                                           @Param("groupName") String groupName,
                                                           @Param("hostName") String name,
                                                           @Param("poolRecordType") int type);

  /**
   * @throws UltraDNSException with code {@link UltraDNSException#POOL_RECORD_ALREADY_EXISTS}.
   */
  @RequestLine("POST")
  String addDirectionalPoolRecord(@Param("record") DirectionalRecord toCreate,
                                  @Param("group") DirectionalGroup group,
                                  @Param("poolId") String poolId);

  /**
   * @throws UltraDNSException with code {@link UltraDNSException#RESOURCE_RECORD_ALREADY_EXISTS}.
   */
  @RequestLine("POST")
  void updateDirectionalPoolRecord(@Param("record") DirectionalRecord update,
                                   @Param("group") DirectionalGroup group);

  @RequestLine("POST")
  @Body("<v01:getDirectionalPoolsOfZone><zoneName>{zoneName}</zoneName></v01:getDirectionalPoolsOfZone>")
  Map<String, String> getDirectionalPoolsOfZone(@Param("zoneName") String zoneName);

  @RequestLine("POST")
  @Body("<v01:getDirectionalDNSRecordsForHost><zoneName>{zoneName}</zoneName><hostName>{hostName}</hostName><poolRecordType>{poolRecordType}</poolRecordType></v01:getDirectionalDNSRecordsForHost>")
  List<DirectionalRecord> getDirectionalDNSRecordsForHost(@Param("zoneName") String zoneName,
                                                          @Param("hostName") String name,
                                                          @Param("poolRecordType") int rrType);

  @RequestLine("POST")
  @Body("<v01:addDirectionalPool><transactionID /><AddDirectionalPoolData dirPoolType=\"GEOLOCATION\" poolRecordType=\"{poolRecordType}\" zoneName=\"{zoneName}\" hostName=\"{hostName}\" description=\"{poolRecordType}\"/></v01:addDirectionalPool>")
  String addDirectionalPool(@Param("zoneName") String zoneName, @Param("hostName") String name,
                            @Param("poolRecordType") String type);

  @RequestLine("POST")
  @Body("<v01:deleteDirectionalPoolRecord><transactionID /><dirPoolRecordId>{dirPoolRecordId}</dirPoolRecordId></v01:deleteDirectionalPoolRecord>")
  void deleteDirectionalPoolRecord(@Param("dirPoolRecordId") String id);

  @RequestLine("POST")
  @Body("<v01:deleteDirectionalPool><transactionID /><dirPoolID>{dirPoolID}</dirPoolID><retainRecordID /></v01:deleteDirectionalPool>")
  void deleteDirectionalPool(@Param("dirPoolID") String dirPoolID);

  enum NetworkStatus {
    GOOD, FAILED;
  }

  class Record {

    String id;
    Long created;
    String name;
    int typeCode;
    int ttl;
    List<String> rdata = new ArrayList<String>();
  }

  class NameAndType {

    String name;
    String type;

    @Override
    public int hashCode() {
      return 37 * name.hashCode() + type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !(obj instanceof NameAndType)) {
        return false;
      }
      NameAndType that = NameAndType.class.cast(obj);
      return this.name.equals(that.name) && this.type.equals(that.type);
    }

    @Override
    public String toString() {
      return "NameAndType(" + name + "," + type + ")";
    }
  }

  class DirectionalGroup {

    String name;
    Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();
  }

  class DirectionalRecord extends Record {

    String geoGroupId;
    String geoGroupName;
    boolean noResponseRecord;

    String type;
  }
}
