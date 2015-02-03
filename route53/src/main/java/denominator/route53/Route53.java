package denominator.route53;

import java.util.ArrayList;
import java.util.List;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

interface Route53 {

  @RequestLine("GET /2012-12-12/hostedzone")
  ZoneList listHostedZones();

  @RequestLine("GET /2012-12-12/hostedzone?marker={marker}")
  ZoneList listHostedZones(@Param("marker") String marker);

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

  static class ZoneList extends ArrayList<Zone> {

    private static final long serialVersionUID = 1L;
    String next;
  }

  static class ResourceRecordSetList extends ArrayList<ResourceRecordSet<?>> {

    private static final long serialVersionUID = 1L;
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

  static class ActionOnResourceRecordSet {

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
