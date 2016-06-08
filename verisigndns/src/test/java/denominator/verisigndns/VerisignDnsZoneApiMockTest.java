package denominator.verisigndns;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.ZoneApi;
import denominator.model.Zone;

public class VerisignDnsZoneApiMockTest {

  @Rule
  public final MockVerisignDnsServer server = new MockVerisignDnsServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "   </ns3:zoneInfo>" + "</ns3:getZoneListRes>"));

    server
        .enqueue(new MockResponse()
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
                + "    </ns3:primaryZoneInfo>" + " </ns3:getZoneInfoRes>"));
    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io"));
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<api1:getZoneList></api1:getZoneList>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {

    server
        .enqueue(new MockResponse()
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
                + "    </ns3:primaryZoneInfo>" + " </ns3:getZoneInfoRes>"));
    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io")).containsExactly(
        Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io"));
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<ns3:getZoneInfoRes></ns3:getZoneInfoRes>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueError("ERROR_OPERATION_FAILURE",
        "Domain already exists. Please verify your domain name.");
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io");
    api.put(zone);
  }

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());
    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server
        .enqueue(new MockResponse()
            .setBody("<ns3:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
                + "   <ns3:callSuccess>true</ns3:callSuccess>" 
                + "</ns3:dnsaWSRes>"));

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete("test.io");
  }
}
