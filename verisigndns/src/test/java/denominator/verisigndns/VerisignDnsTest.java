package denominator.verisigndns;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.Live;
import denominator.Live.UseTestGraph;
import denominator.ZoneApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

@RunWith(Live.class)
@UseTestGraph(VerisignDnsTestGraph.class)
public class VerisignDnsTest {

  @Parameter
  public DNSApiManager manager;

  @Test
  public void zoneTest() {
    ZoneApi zoneApi = manager.api().zones();

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".io";
    int ttl = 86400;
    String email = "user@" + zoneName;

    // createZone
    System.out.println("\nCreating zone...");
    zoneApi.put(Zone.create(null, zoneName, ttl, email));

    // getZoneInfo
    System.out.println("\nQuerying zone by name...");
    Iterator<Zone> zoneIterator = zoneApi.iterateByName(zoneName);
    while (zoneIterator.hasNext()) {
      System.out.printf("\t%s", zoneIterator.next());
      System.out.println();
    }

    // getZoneList
    System.out.println("\nQuerying zones for an account...");
    zoneIterator = zoneApi.iterator();
    int count = 0;
    while (zoneIterator.hasNext()) {
      zoneIterator.next();
      count++;
    }
    System.out.println("\tZone Size:" + count);

    // deleteZone
    System.out.println("Deleting zone...");
    zoneApi.delete(zoneName);
  }

  @Test
  public void rrSetTest() {

    // Setup test data
    String zoneName = "testzone-" + System.currentTimeMillis() + ".io";
    int ttl = 86400;
    String email = "user@" + zoneName;

    // createZone
    System.out.println("\nCreating zone...");
    ZoneApi zoneApi = manager.api().zones();
    String zoneId = zoneApi.put(Zone.create(null, zoneName, ttl, email));

    AllProfileResourceRecordSetApi recordSetsInZoneApi = manager.api().recordSetsInZone(zoneId);

    // Add ResourceRecord record
    System.out.println("\nAdding resource records...");

    // Add A record
    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A")
        .add(Util.toMap("A", "127.0.0.1")).build());

    // Add TLSA record
    recordSetsInZoneApi.put(ResourceRecordSet
        .builder()
        .name("_443._tcp.www")
        .type("TLSA")
        .add(
            Util.toMap("CERT",
                "3 1 1 b760c12119c388736da724df1224d21dfd23bf03366c286de1a4125369ef7de0")).build());

    // getResourceRecords
    System.out.println("\nQuerying resource records...");
    Iterator<ResourceRecordSet<?>> rrsIterator = recordSetsInZoneApi.iterator();
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // getResourceRecordByName
    System.out.println("\nQuerying resource record by name...");
    rrsIterator = recordSetsInZoneApi.iterateByName("www");
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // getResourceRecordByNameAndType
    System.out.println("\nQuerying resource record by name and rrType...");
    rrsIterator = recordSetsInZoneApi.iterateByNameAndType("www", "A");
    while (rrsIterator.hasNext()) {
      ResourceRecordSet<?> rrs = rrsIterator.next();
      System.out.printf("\t%s", rrs.toString());
      System.out.println();
    }

    // delete Resource Record
    System.out.println("\nDeleting resource record...");
    recordSetsInZoneApi.deleteByNameAndType("www", "A");

    // deleteZone
    System.out.println("Deleting zone...");
    zoneApi.delete(zoneName);
  }

  static MockResponse getZoneListRes =
      new MockResponse()
          .setBody("<ns3:getZoneListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
              + "   <ns3:callSuccess>true</ns3:callSuccess>"
              + "   <ns3:totalCount>1</ns3:totalCount>"
              + "     <ns3:zoneInfo>"
              + "       <ns3:domainName>denominator.io</ns3:domainName>"
              + "       <ns3:type>DNS Hosting</ns3:type>"
              + "       <ns3:status>ACTIVE</ns3:status>"
              + "       <ns3:createTimestamp>2015-09-29T01:55:39.000Z</ns3:createTimestamp>"
              + "       <ns3:updateTimestamp>2015-09-30T00:25:53.000Z</ns3:updateTimestamp>"
              + "       <ns3:geoLocationEnabled>No</ns3:geoLocationEnabled>"
              + "   </ns3:zoneInfo>"
              + "</ns3:getZoneListRes>");

  static MockResponse getZoneInfoRes =
      new MockResponse()
          .setBody(" <ns3:getZoneInfoRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
              + "    <ns3:callSuccess>true</ns3:callSuccess>"
              + "    <ns3:primaryZoneInfo>"
              + "  <ns3:domainName>denominator.io</ns3:domainName>"
              + "  <ns3:type>DNS Hosting</ns3:type>"
              + "  <ns3:status>ACTIVE</ns3:status>"
              + "  <ns3:createTimestamp>2015-09-29T13:58:53.000Z</ns3:createTimestamp>"
              + "  <ns3:updateTimestamp>2015-09-29T14:41:11.000Z</ns3:updateTimestamp>"
              + "  <ns3:zoneSOAInfo>"
              + "     <ns3:email>nil@denominator.io</ns3:email>"
              + "     <ns3:retry>7400</ns3:retry>"
              + "     <ns3:ttl>86400</ns3:ttl>"
              + "     <ns3:refresh>30000</ns3:refresh>"
              + "     <ns3:expire>1234567</ns3:expire>"
              + "     <ns3:serial>1443535137</ns3:serial>"
              + "  </ns3:zoneSOAInfo>"
              + "  <ns3:serviceLevel>COMPLETE</ns3:serviceLevel>"
              + "  <ns3:webParking>"
              + "     <ns3:parkingEnabled>false</ns3:parkingEnabled>"
              + "  </ns3:webParking>"
              + "  <ns3:verisignNSInfo>"
              + "     <ns3:virtualNameServerId>10</ns3:virtualNameServerId>"
              + "     <ns3:name>a1.verisigndns.com</ns3:name>"
              + "     <ns3:ipAddress>209.112.113.33</ns3:ipAddress>"
              + "     <ns3:ipv6Address>2001:500:7967::2:33</ns3:ipv6Address>"
              + "     <ns3:location>Anycast Global</ns3:location>"
              + "  </ns3:verisignNSInfo>"
              + "  <ns3:verisignNSInfo>"
              + "     <ns3:virtualNameServerId>11</ns3:virtualNameServerId>"
              + "     <ns3:name>a2.verisigndns.com</ns3:name>"
              + "     <ns3:ipAddress>209.112.114.33</ns3:ipAddress>"
              + "     <ns3:ipv6Address>2620:74:19::33</ns3:ipv6Address>"
              + "     <ns3:location>Anycast 1</ns3:location>"
              + "  </ns3:verisignNSInfo>"
              + "  <ns3:verisignNSInfo>"
              + "     <ns3:virtualNameServerId>12</ns3:virtualNameServerId>"
              + "     <ns3:name>a3.verisigndns.com</ns3:name>"
              + "     <ns3:ipAddress>69.36.145.33</ns3:ipAddress>"
              + "     <ns3:ipv6Address>2001:502:cbe4::33</ns3:ipv6Address>"
              + "     <ns3:location>Anycast 2</ns3:location>"
              + "  </ns3:verisignNSInfo>"
              + "    </ns3:primaryZoneInfo>" + " </ns3:getZoneInfoRes>");

  static MockResponse getResourceRecordListRes =
      new MockResponse()
          .setBody("<ns3:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
              + "   <ns3:callSuccess>true</ns3:callSuccess>"
              + "   <ns3:totalCount>1</ns3:totalCount>"
              + "   <ns3:resourceRecord>"
              + "       <ns3:resourceRecordId>3194811</ns3:resourceRecordId>"
              + "       <ns3:owner>www.denominator.io.</ns3:owner>"
              + "       <ns3:type>A</ns3:type>"
              + "       <ns3:ttl>86400</ns3:ttl>"
              + "       <ns3:rData>127.0.0.1</ns3:rData>"
              + "   </ns3:resourceRecord>"
              + "</ns3:getResourceRecordListRes>");

  static MockResponse twoResourceRecordRes =
      new MockResponse()
          .setBody("<ns3:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
              + "   <ns3:callSuccess>true</ns3:callSuccess>"
              + "   <ns3:totalCount>2</ns3:totalCount>"
              + "   <ns3:resourceRecord>"
              + "       <ns3:resourceRecordId>3194802</ns3:resourceRecordId>"
              + "       <ns3:owner>www.denominator.io.</ns3:owner>"
              + "       <ns3:type>A</ns3:type>"
              + "       <ns3:ttl>86400</ns3:ttl>"
              + "       <ns3:rData>127.0.0.11</ns3:rData>"
              + "   </ns3:resourceRecord>"
              + "   <ns3:resourceRecord>"
              + "       <ns3:resourceRecordId>3194811</ns3:resourceRecordId>"
              + "       <ns3:owner>www1.denominator.io.</ns3:owner>"
              + "       <ns3:type>A</ns3:type>"
              + "       <ns3:ttl>86400</ns3:ttl>"
              + "       <ns3:rData>127.0.0.12</ns3:rData>"
              + "   </ns3:resourceRecord>"
              + "</ns3:getResourceRecordListRes>");
}
