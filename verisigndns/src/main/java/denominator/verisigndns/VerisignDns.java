package denominator.verisigndns;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.verisigndns.VerisignDnsContentHandlers.Page;
import denominator.verisigndns.VerisignDnsContentHandlers.ResourceRecord;
import denominator.verisigndns.VerisignDnsEncoder.GetRRList;
import denominator.verisigndns.VerisignDnsEncoder.Paging;
import feign.Param;
import feign.RequestLine;

interface VerisignDns {

  @RequestLine("POST")
  void createZone(@Param("createZone") Zone zone);

  @RequestLine("POST")
  void updateSoa(@Param("updateSoa") Zone zone);

  @RequestLine("POST")
  void deleteZone(@Param("deleteZone") String zone);

  @RequestLine("POST")
  Page<Zone> getZones(@Param("getZoneList") Paging paging);

  @RequestLine("POST")
  Zone getZone(@Param("getZone") String zone);

  @RequestLine("POST")
  void createResourceRecords(@Param("zone") String zone,
      @Param("rrSet") ResourceRecordSet<?> rrSet, @Param("oldRRSet") ResourceRecordSet<?> oldRRSet);

  @RequestLine("POST")
  void updateResourceRecords(@Param("zone") String zone,
      @Param("rrSet") ResourceRecordSet<?> rrSet, @Param("oldRRSet") ResourceRecordSet<?> oldRRSet);

  @RequestLine("POST")
  Page<ResourceRecord> getResourceRecords(@Param("zone") String zone,
      @Param("getRRList") GetRRList rrRequest);

  @RequestLine("POST")
  void deleteResourceRecords(@Param("zone") String zone,
      @Param("deleteRRSet") ResourceRecordSet<?> rrSet);
}
