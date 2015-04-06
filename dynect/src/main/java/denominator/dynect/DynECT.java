package denominator.dynect;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@Headers({"API-Version: 3.5.10", "Content-Type: application/json"})
public interface DynECT {

  @RequestLine("GET /Zone")
  Data<List<String>> zones();;

  @RequestLine("POST /Zone/{name}")
  @Body("%7B\"ttl\":{ttl},\"rname\":\"{rname}\"%7D")
  void createZone(@Param("name") String name, @Param("ttl") int ttl, @Param("rname") String rname);

  @RequestLine("DELETE /Zone/{name}")
  void deleteZone(@Param("name") String name);

  @RequestLine("PUT /Zone/{zone}")
  @Body("{\"publish\":true}")
  void publish(@Param("zone") String zone);

  @RequestLine("GET /AllRecord/{zone}?detail=Y")
  Data<Iterator<ResourceRecordSet<?>>> rrsets(@Param("zone") String zone);

  @RequestLine("POST /CheckPermissionReport")
  @Body("{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}")
  Data<Boolean> hasAllGeoPermissions();

  @RequestLine("GET /Geo?detail=Y")
  Data<List<GeoService>> geoServices();

  @RequestLine("GET /AllRecord/{zone}/{fqdn}?detail=Y")
  Data<Iterator<ResourceRecordSet<?>>> rrsetsInZoneByName(@Param("zone") String zone,
                                                          @Param("fqdn") String fqdn);

  @RequestLine("GET /{type}Record/{zone}/{fqdn}?detail=Y")
  Data<Iterator<ResourceRecordSet<?>>> rrsetsInZoneByNameAndType(@Param("zone") String zone,
                                                                 @Param("fqdn") String fqdn,
                                                                 @Param("type") String type);

  @RequestLine("GET /{type}Record/{zone}/{fqdn}?detail=Y")
  Data<Iterator<Record>> recordsInZoneByNameAndType(@Param("zone") String zone,
                                                    @Param("fqdn") String fqdn,
                                                    @Param("type") String type);

  @RequestLine("POST /{type}Record/{zone}/{fqdn}")
  void scheduleCreateRecord(@Param("zone") String zone, @Param("fqdn") String fqdn,
                            @Param("type") String type, @Param("ttl") int ttl,
                            @Param("rdata") Map<String, Object> rdata);

  @RequestLine("PUT /SOARecord/{zone}/{zone}/{recordId}")
  @Body("%7B\"ttl\":\"{ttl}\",\"rdata\":%7B\"rname\":\"{rname}\"%7D%7D")
  void scheduleUpdateSOA(@Param("zone") String zone, @Param("recordId") long recordId,
                         @Param("ttl") int ttl, @Param("rname") String rname);

  @RequestLine("DELETE /{recordId}")
  void scheduleDeleteRecord(@Param("recordId") String recordId);

  @RequestLine("DELETE /{type}Record/{zone}/{fqdn}")
  void scheduleDeleteRecordsInZoneByNameAndType(@Param("zone") String zone,
                                                @Param("fqdn") String fqdn,
                                                @Param("type") String type);

  /**
   * DynECT json includes an envelope called "data", which makes it difficult.
   */
  class Data<T> {

    T data;
  }

  class Record {

    long id;
    String serviceClass;
    String name;
    String type;
    int ttl;
    Map<String, Object> rdata = new LinkedHashMap<String, Object>();
  }

  class GeoService {

    List<Node> nodes = new ArrayList<Node>();
    List<GeoRegionGroup> groups = new ArrayList<GeoRegionGroup>();

    static class Node {

      String zone;
      String fqdn;
    }

    static class GeoRegionGroup {

      String service_name;
      String name;
      // aaaa_weight
      Map<String, List<Integer>> weight = new LinkedHashMap<String, List<Integer>>();
      List<String> countries = new ArrayList<String>();
      // spf_rdata
      Map<String, List<JsonElement>> rdata = new LinkedHashMap<String, List<JsonElement>>();
      // dhcid_ttl
      Map<String, Integer> ttl = new LinkedHashMap<String, Integer>();
    }
  }
}
