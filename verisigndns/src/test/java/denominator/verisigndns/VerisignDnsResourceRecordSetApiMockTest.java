package denominator.verisigndns;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.AllProfileResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;

public class VerisignDnsResourceRecordSetApiMockTest {

  @Rule
  public final MockVerisignDnsServer server = new MockVerisignDnsServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));
    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");

    assertThat(recordSetsInZoneApi.iterator()).containsExactly(
        ResourceRecordSet.builder().name("www").type("A").ttl(86400)
            .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse()
        .setBody("<ns3:getResourceRecordListRes></ns3:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");

    assertThat(recordSetsInZoneApi.iterateByName("www")).containsExactly(
        ResourceRecordSet.builder().name("www").type("A").ttl(86400)
            .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse()
        .setBody("<ns3:getResourceRecordListRes></ns3:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();
  }

  @Test
  public void putFirstRecordCreatesNewRRSet() throws Exception {
    server
        .enqueue(new MockResponse()
            .setBody("<ns3:getResourceRecordListRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
                + "   <ns3:callSuccess>true</ns3:callSuccess>"
                + "   <ns3:totalCount>0</ns3:totalCount>" + "</ns3:getResourceRecordListRes>"));
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).isEmpty();

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void putSameRecordNoOp() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());

    assertThat(recordSetsInZoneApi.iterator()).hasSize(1);
  }

  @Test
  public void putOneRecordReplacesRRSet() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    assertThat(recordSetsInZoneApi.iterator()).hasSize(2);

    recordSetsInZoneApi.put(ResourceRecordSet.builder().name("www").type("A").ttl(86400)
        .add(Util.toMap("A", "127.0.0.1")).build());
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));
    server
        .enqueue(new MockResponse()
            .setBody("<ns3:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
                + "   <ns3:callSuccess>true</ns3:callSuccess>" + "</ns3:dnsaWSRes>"));

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    recordSetsInZoneApi.deleteByNameAndType("www.denominator.io.", "A");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server
        .enqueue(new MockResponse()
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
                + "</ns3:getResourceRecordListRes>"));
    server.enqueue(new MockResponse());

    AllProfileResourceRecordSetApi recordSetsInZoneApi =
        server.connect().api().recordSetsInZone("denominator.io");
    recordSetsInZoneApi.deleteByNameAndType("www", "A");
  }
}
