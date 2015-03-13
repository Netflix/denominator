package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import denominator.ResourceRecordSetApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.ultradns.UltraDNSException.POOL_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSTest.deleteLBPool;
import static denominator.ultradns.UltraDNSTest.deleteLBPoolResponse;
import static denominator.ultradns.UltraDNSTest.deleteResourceRecord;
import static denominator.ultradns.UltraDNSTest.deleteResourceRecordResponse;
import static denominator.ultradns.UltraDNSTest.getLoadBalancingPoolsByZone;
import static denominator.ultradns.UltraDNSTest.getLoadBalancingPoolsByZoneResponseFooter;
import static denominator.ultradns.UltraDNSTest.getLoadBalancingPoolsByZoneResponseHeader;
import static denominator.ultradns.UltraDNSTest.getRRPoolRecords;
import static denominator.ultradns.UltraDNSTest.getRRPoolRecordsResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByType;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByTypeResponseFooter;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByTypeResponseHeader;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZone;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseFooter;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseHeader;
import static java.lang.String.format;

public class UltraDNSResourceRecordSetApiMockTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();

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
      getResourceRecordsOfDNameByTypeAll.replace("<rrType>0</rrType>", "<rrType>1</rrType>");
  static String
      addRRLBPoolTemplate =
      "<v01:addRRLBPool><transactionID /><zoneName>denominator.io.</zoneName><hostName>www.denominator.io.</hostName><description>%s</description><poolRecordType>%s</poolRecordType><rrGUID /></v01:addRRLBPool>";
  static String
      addRRLBPoolResponseTemplate =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRRLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><RRPoolID xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</RRPoolID></ns1:addRRLBPoolResponse></soap:Body></soap:Envelope>";
  static String
      addRecordToRRPoolTemplate =
      "<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"%s\" info1Value=\"%s\" ZoneName=\"denominator.io.\" Type=\"%s\" TTL=\"%s\"/></v01:addRecordToRRPool>";
  static String
      addRecordToRRPoolResponseTemplate =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRecordToRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><guid xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</guid></ns1:addRecordToRRPoolResponse></soap:Body></soap:Envelope>";
  static String poolIDTypeTemplate = "      <ns2:LBPoolData zoneid=\"0000000000000001\">\n"
                                     + "        <ns2:PoolData description=\"foo\" PoolId=\"%s\" PoolType=\"RD\" PoolRecordType=\"%s\" PoolDName=\"www.denominator.io.\" ResponseMethod=\"RR\" />\n"
                                     + "      </ns2:LBPoolData>\n";
  static String poolsForAandAAAA = new StringBuilder(getLoadBalancingPoolsByZoneResponseHeader)
      .append(format(poolIDTypeTemplate, "1111A", "A"))
      .append(format(poolIDTypeTemplate, "1111AAAA", "AAAA"))
      .append(getLoadBalancingPoolsByZoneResponseFooter).toString();
  static String record1 = getResourceRecordsOfDNameByTypeResponseHeader
                          + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"6\" DName=\"www.denominator.io.\" TTL=\"3600\" Guid=\"ABCDEF\" ZoneId=\"03053D8E57C7A22A\" LName=\"www.denominator.io.\" Created=\"2013-02-22T08:22:48.000Z\" Modified=\"2013-02-22T08:22:49.000Z\">\n"
                          + "          <ns2:InfoValues Info1Value=\"192.0.2.1\" />\n"
                          + "        </ns2:ResourceRecord>\n"
                          + getResourceRecordsOfDNameByTypeResponseFooter;

  @Test
  public void listWhenNoneMatch() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");

    assertThat(api.iterator()).isEmpty();

    server.assertSoapBody(getResourceRecordsOfZone);
  }

  @Test
  public void iterateByNameWhenNoneMatch() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    assertThat(api.iterateByName("www.denominator.io.")).isEmpty();

    server.assertSoapBody(getResourceRecordsOfDNameByTypeAll);
  }

  @Test
  public void iterateByNameWhenMatch() throws Exception {
    server.enqueue(new MockResponse().setBody(records1And2));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");

    assertThat(api.iterateByName("www.denominator.io."))
        .containsExactly(
            a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeAll);
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");

    assertThat(api.getByNameAndType("www.denominator.io.", "A")).isNull();

    server.assertSoapBody(getResourceRecordsOfDNameByTypeA);
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(records1And2));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");

    assertThat(api.getByNameAndType("www.denominator.io.", "A"))
        .isEqualTo(a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeA);
  }

  @Test
  public void putFirstACreatesRoundRobinPoolThenAddsRecordToIt() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueue(new MockResponse().setBody(format(addRRLBPoolResponseTemplate, "1111A")));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");

    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeA);
    server.assertSoapBody(format(addRRLBPoolTemplate, "1", "1"));
    server.assertSoapBody(format(addRecordToRRPoolTemplate, "1111A", "192.0.2.1", "1", 3600));
  }

  @Test
  public void putFirstAReusesExistingEmptyRoundRobinPool()
      throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueueError(POOL_ALREADY_EXISTS,
                        "Pool already created for this host name : www.denominator.io.");
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeA);
    server.assertSoapBody(format(addRRLBPoolTemplate, "1", "1"));
    server.assertSoapBody(getLoadBalancingPoolsByZone);
    server.assertSoapBody(format(addRecordToRRPoolTemplate, "1111A", "192.0.2.1", "1", 3600));
  }

  @Test
  public void deleteAlsoRemovesPool() throws Exception {
    server.enqueue(new MockResponse().setBody(record1));
    server.enqueue(new MockResponse().setBody(deleteResourceRecordResponse));
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(new MockResponse().setBody(getRRPoolRecordsResponseAbsent));
    server.enqueue(new MockResponse().setBody(deleteLBPoolResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.deleteByNameAndType("www.denominator.io.", "A");

    server.assertSoapBody(getResourceRecordsOfDNameByTypeA);
    server.assertSoapBody(deleteResourceRecord);
    server.assertSoapBody(getLoadBalancingPoolsByZone);
    server.assertSoapBody(getRRPoolRecords.replace("000000000000002", "1111A"));
    server.assertSoapBody(deleteLBPool.replace("AAAAAAAAAAAAAAAA", "1111A"));
  }

  @Test
  public void putSecondAAddsRecordToExistingPool() throws Exception {
    server.enqueue(new MockResponse().setBody(record1));
    server.enqueueError(POOL_ALREADY_EXISTS,
                        "Pool already created for this host name : www.denominator.io.");
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "BBBBBBBBBBBB")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.put(a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeA);
    server.assertSoapBody(format(addRRLBPoolTemplate, "1", "1"));
    server.assertSoapBody(getLoadBalancingPoolsByZone);
    server.assertSoapBody(
        format(addRecordToRRPoolTemplate, "1111A", "198.51.100.1", "1", 3600));
  }

  @Test
  public void putFirstAAAACreatesRoundRobinPoolThenAddsRecordToIt() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueue(new MockResponse().setBody(format(addRRLBPoolResponseTemplate, "1111AAAA")));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.put(aaaa("www.denominator.io.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeAll
                              .replace("<rrType>0</rrType>", "<rrType>28</rrType>"));
    server.assertSoapBody(format(addRRLBPoolTemplate, "28", "28"));
    server.assertSoapBody(
        format(addRecordToRRPoolTemplate, "1111AAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
               "28", 3600));
  }

  @Test
  public void putFirstAAAAReusesExistingEmptyRoundRobinPool() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
    server.enqueueError(POOL_ALREADY_EXISTS,
                        "Pool already created for this host name : www.denominator.io.");
    server.enqueue(new MockResponse().setBody(poolsForAandAAAA));
    server.enqueue(
        new MockResponse().setBody(format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");

    api.put(aaaa("www.denominator.io.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));

    server.assertSoapBody(getResourceRecordsOfDNameByTypeAll
                              .replace("<rrType>0</rrType>", "<rrType>28</rrType>"));
    server.assertSoapBody(format(addRRLBPoolTemplate, "28", "28"));
    server.assertSoapBody(getLoadBalancingPoolsByZone);
    server.assertSoapBody(
        format(addRecordToRRPoolTemplate, "1111AAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
               "28", 3600));
  }
}
