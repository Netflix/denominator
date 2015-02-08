package denominator.ultradns;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.Denominator;
import denominator.ResourceRecordSetApi;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.ultradns.UltraDNSTest.SOAP_TEMPLATE;
import static denominator.ultradns.UltraDNSTest.deleteLBPool;
import static denominator.ultradns.UltraDNSTest.deleteLBPoolResponse;
import static denominator.ultradns.UltraDNSTest.deleteResourceRecord;
import static denominator.ultradns.UltraDNSTest.deleteResourceRecordResponse;
import static denominator.ultradns.UltraDNSTest.getLoadBalancingPoolsByZone;
import static denominator.ultradns.UltraDNSTest.getLoadBalancingPoolsByZoneResponseFooter;
import static denominator.ultradns.UltraDNSTest.getLoadBalancingPoolsByZoneResponseHeader;
import static denominator.ultradns.UltraDNSTest.getRRPoolRecords;
import static denominator.ultradns.UltraDNSTest.getRRPoolRecordsResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getRRPoolRecordsResponseFooter;
import static denominator.ultradns.UltraDNSTest.getRRPoolRecordsResponseHeader;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByType;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByTypeResponseFooter;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByTypeResponseHeader;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZone;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseFooter;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseHeader;
import static denominator.ultradns.UltraDNSTest.poolAlreadyExists;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

@Test
public class UltraDNSResourceRecordSetApiMockTest {

  static String
      getResourceRecordsOfDNameByTypeAll =
      getResourceRecordsOfDNameByType.replace("<rrType>6</rrType>",
                                              "<rrType>0</rrType>")
          .replace("<hostName>denominator.io.", "<hostName>www.denominator.io.");
  static String
      aRecordTTLGuidAddressTemplate =
      "<ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"%d\" Guid=\"%s\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2011-09-27T23:49:22.000Z\"><ns2:InfoValues Info1Value=\"%s\"/></ns2:ResourceRecord>";
  static String records1And2 = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
      .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "192.0.2.1"))
      .append(format(aRecordTTLGuidAddressTemplate, 3600, "BBBBBBBBBBBB", "198.51.100.1"))
      .append(getResourceRecordsOfZoneResponseFooter).toString();
  static String
      getResourceRecordsOfDNameByTypeA =
      getResourceRecordsOfDNameByTypeAll.replace("<rrType>0</rrType>",
                                                 "<rrType>1</rrType>");
  static String addRRLBPoolTemplate = format(
      SOAP_TEMPLATE,
      "<v01:addRRLBPool><transactionID /><zoneName>denominator.io.</zoneName><hostName>www.denominator.io.</hostName><description>%s</description><poolRecordType>%s</poolRecordType><rrGUID /></v01:addRRLBPool>");
  static String
      addRRLBPoolResponseTemplate =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRRLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><RRPoolID xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</RRPoolID></ns1:addRRLBPoolResponse></soap:Body></soap:Envelope>";
  static String addRecordToRRPoolTemplate = format(
      SOAP_TEMPLATE,
      "<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"%s\" info1Value=\"%s\" ZoneName=\"denominator.io.\" Type=\"%s\" TTL=\"%s\"/></v01:addRecordToRRPool>");
  static String
      addRecordToRRPoolResponseTemplate =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRecordToRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><guid xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</guid></ns1:addRecordToRRPoolResponse></soap:Body></soap:Envelope>";
  static String poolIDTypeTemplate = ""//
                                     + "      <ns2:LBPoolData zoneid=\"0000000000000001\">\n"//
                                     + "        <ns2:PoolData description=\"foo\" PoolId=\"%s\" PoolType=\"RD\" PoolRecordType=\"%s\" PoolDName=\"www.denominator.io.\" ResponseMethod=\"RR\" />\n"
//
                                     + "      </ns2:LBPoolData>\n";
  static String poolsForAandAAAA = new StringBuilder(getLoadBalancingPoolsByZoneResponseHeader)
      .append(format(poolIDTypeTemplate, "1111A", "A"))//
      .append(format(poolIDTypeTemplate, "1111AAAA", "AAAA"))//
      .append(getLoadBalancingPoolsByZoneResponseFooter).toString();
  static String record1 = ""//
                          + getResourceRecordsOfDNameByTypeResponseHeader//
                          + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"6\" DName=\"www.denominator.io.\" TTL=\"3600\" Guid=\"ABCDEF\" ZoneId=\"03053D8E57C7A22A\" LName=\"www.denominator.io.\" Created=\"2013-02-22T08:22:48.000Z\" Modified=\"2013-02-22T08:22:49.000Z\">\n"
//
                          + "          <ns2:InfoValues Info1Value=\"192.0.2.1\" />\n"//
                          + "        </ns2:ResourceRecord>\n"//
                          + getResourceRecordsOfDNameByTypeResponseFooter;
  static String pooledRecord1 = new StringBuilder(getRRPoolRecordsResponseHeader)
      .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "192.0.2.1"))
      .append(getRRPoolRecordsResponseFooter).toString();

  private static ResourceRecordSetApi mockApi(final int port) {
    return Denominator.create(new UltraDNSProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port + "/";
      }
    }, credentials("joe", "letmein")).api().basicRecordSetsInZone("denominator.io.");
  }

  @Test
  public void listWhenNoneMatch() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertFalse(api.iterator().hasNext());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfZone);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iterateByNameWhenNoneMatch() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertFalse(api.iterateByName("www.denominator.io.").hasNext());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeAll);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iterateByNameWhenMatch() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(records1And2));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertEquals(api.iterateByName("www.denominator.io.").next(),
                   a("www.denominator.io.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeAll);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertNull(api.getByNameAndType("www.denominator.io.", "A"));

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeA);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(records1And2));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertEquals(api.getByNameAndType("www.denominator.io.", "A"),
                   a("www.denominator.io.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeA);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putFirstACreatesRoundRobinPoolThenAddsRecordToIt()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueue(new MockResponse().setBody(format(addRRLBPoolResponseTemplate, "1111A")));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

      assertEquals(server.getRequestCount(), 3);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeA);
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRRLBPoolTemplate, "1", "1"));
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRecordToRRPoolTemplate, "1111A", "192.0.2.1", "1", 3600));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putFirstAReusesExistingEmptyRoundRobinPool()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueue(new MockResponse().setResponseCode(500).setBody(poolAlreadyExists));
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

      assertEquals(server.getRequestCount(), 4);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeA);
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRRLBPoolTemplate, "1", "1"));
      assertEquals(new String(server.takeRequest().getBody()), getLoadBalancingPoolsByZone);
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRecordToRRPoolTemplate, "1111A", "192.0.2.1", "1", 3600));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void deleteAlsoRemovesPool() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(record1));
    server.enqueue(new MockResponse().setBody(deleteResourceRecordResponse));
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(new MockResponse().setBody(getRRPoolRecordsResponseAbsent));
    server.enqueue(new MockResponse().setBody(deleteLBPoolResponse));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.deleteByNameAndType("www.denominator.io.", "A");

      assertEquals(server.getRequestCount(), 5);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeA);
      assertEquals(new String(server.takeRequest().getBody()), deleteResourceRecord);
      assertEquals(new String(server.takeRequest().getBody()), getLoadBalancingPoolsByZone);
      assertEquals(new String(server.takeRequest().getBody()),
                   getRRPoolRecords.replace("000000000000002", "1111A"));
      assertEquals(new String(server.takeRequest().getBody()),
                   deleteLBPool.replace("AAAAAAAAAAAAAAAA", "1111A"));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putSecondAAddsRecordToExistingPool() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(record1));
    server.enqueue(new MockResponse().setResponseCode(500).setBody(poolAlreadyExists));
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "BBBBBBBBBBBB")));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, ImmutableSet.of("192.0.2.1", "198.51.100.1")));

      assertEquals(server.getRequestCount(), 4);
      assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfDNameByTypeA);
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRRLBPoolTemplate, "1", "1"));
      assertEquals(new String(server.takeRequest().getBody()), getLoadBalancingPoolsByZone);
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRecordToRRPoolTemplate, "1111A", "198.51.100.1", "1", 3600));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putFirstAAAACreatesRoundRobinPoolThenAddsRecordToIt()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueue(new MockResponse().setBody(format(addRRLBPoolResponseTemplate, "1111AAAA")));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(aaaa("www.denominator.io.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));

      assertEquals(server.getRequestCount(), 3);
      assertEquals(new String(server.takeRequest().getBody()),
                   getResourceRecordsOfDNameByTypeAll
                       .replace("<rrType>0</rrType>", "<rrType>28</rrType>"));
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRRLBPoolTemplate, "28", "28"));
      assertEquals(
          new String(server.takeRequest().getBody()),
          format(addRecordToRRPoolTemplate, "1111AAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
                 "28", 3600));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putFirstAAAAReusesExistingEmptyRoundRobinPool()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueue(new MockResponse().setResponseCode(500).setBody(poolAlreadyExists));
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(aaaa("www.denominator.io.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));

      assertEquals(server.getRequestCount(), 4);
      assertEquals(new String(server.takeRequest().getBody()),
                   getResourceRecordsOfDNameByTypeAll
                       .replace("<rrType>0</rrType>", "<rrType>28</rrType>"));
      assertEquals(new String(server.takeRequest().getBody()),
                   format(addRRLBPoolTemplate, "28", "28"));
      assertEquals(new String(server.takeRequest().getBody()), getLoadBalancingPoolsByZone);
      assertEquals(
          new String(server.takeRequest().getBody()),
          format(addRecordToRRPoolTemplate, "1111AAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
                 "28", 3600));
    } finally {

      server.shutdown();
    }
  }
}
