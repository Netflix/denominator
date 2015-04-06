package denominator.route53;

import java.util.ArrayList;
import java.util.List;

import denominator.model.ResourceRecordSet;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

interface Route53 {

  @RequestLine("GET /2012-12-12/hostedzone/{zoneId}")
  NameAndCount getHostedZone(@Param("zoneId") String zoneId);

  @RequestLine("GET /2012-12-12/hostedzone")
  HostedZoneList listHostedZones();

  @RequestLine("GET /2012-12-12/hostedzone?marker={marker}")
  HostedZoneList listHostedZones(@Param("marker") String marker);

  @RequestLine("GET /2013-04-01/hostedzonesbyname?dnsname={dnsname}")
  HostedZoneList listHostedZonesByName(@Param("dnsname") String dnsname);

  @RequestLine("POST /2012-12-12/hostedzone")
  @Headers("Content-Type: application/xml")
  @Body("<CreateHostedZoneRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><Name>{name}</Name><CallerReference>{reference}</CallerReference></CreateHostedZoneRequest>")
  HostedZoneList createHostedZone(@Param("name") String name, @Param("reference") String reference);

  @RequestLine("DELETE /2012-12-12/hostedzone/{zoneId}")
  HostedZoneList deleteHostedZone(@Param("zoneId") String zoneId);

  @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset")
  ResourceRecordSetList listResourceRecordSets(@Param("zoneId") String zoneId);

  @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}")
  ResourceRecordSetList listResourceRecordSets(@Param("zoneId") String zoneId,
                                               @Param("name") String name);

  @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}&type={type}")
  ResourceRecordSetList listResourceRecordSets(@Param("zoneId") String zoneId,
                                               @Param("name") String name,
                                               @Param("type") String type);

  @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}&type={type}&identifier={identifier}")
  ResourceRecordSetList listResourceRecordSets(@Param("zoneId") String zoneId,
                                               @Param("name") String name,
                                               @Param("type") String type,
                                               @Param("identifier") String identifier);

  @RequestLine("POST /2012-12-12/hostedzone/{zoneId}/rrset")
  @Headers("Content-Type: application/xml")
  void changeResourceRecordSets(@Param("zoneId") String zoneId,
                                List<ActionOnResourceRecordSet> changes)
      throws InvalidChangeBatchException;


  class NameAndCount {
    String name;
    int resourceRecordSetCount;
  }

  class HostedZone {
    String id;
    String name;
  }

  class HostedZoneList extends ArrayList<HostedZone> {

    public String next;
  }

  class ResourceRecordSetList extends ArrayList<ResourceRecordSet<?>> {
    NextRecord next;

    static class NextRecord {

      final String name;
      String type;
      String identifier;

      NextRecord(String name) {
        this.name = name;
      }
    }
  }

  class ActionOnResourceRecordSet {

    final String action;
    final ResourceRecordSet<?> rrs;

    private ActionOnResourceRecordSet(String action, ResourceRecordSet<?> rrs) {
      this.action = action;
      this.rrs = rrs;
    }

    static ActionOnResourceRecordSet create(ResourceRecordSet<?> rrs) {
      return new ActionOnResourceRecordSet("CREATE", rrs);
    }

    static ActionOnResourceRecordSet delete(ResourceRecordSet<?> rrs) {
      return new ActionOnResourceRecordSet("DELETE", rrs);
    }
  }
}
