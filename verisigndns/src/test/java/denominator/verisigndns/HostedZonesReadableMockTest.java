package denominator.verisigndns;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.DNSApiManager;

public class HostedZonesReadableMockTest {

  @Rule
  public final MockVerisignDnsServer server = new MockVerisignDnsServer();

  @Test
  public void singleRequestOnSuccess() throws Exception {
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

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());

    server.assertRequest();
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertRequest();
  }
}
