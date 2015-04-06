package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;
import denominator.model.rdata.SOAData;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.clouddns.RackspaceApisTest.domainId;
import static denominator.clouddns.RackspaceApisTest.soaResponse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class CloudDNSResourceRecordSetApiMockTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

  String
      records =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703385\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703386\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"}],\"totalEntries\":3}";
  String
      recordsPage1 =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703385\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703386\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"}],\"totalEntries\":3,\"links\":[{\"href\":\"URL/v1.0/123123/domains/1234/records?limit=3&offset=3\",\"rel\":\"next\"}]}";
  String
      recordsPage2 =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703385\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703386\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"}],\"totalEntries\":3,\"links\":[{\"href\":\"URL/v1.0/123123/domains/1234/records?limit=3&offset=0\",\"rel\":\"previous\"}]}";
  String
      recordsByName =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703385\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-8703386\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"}],\"totalEntries\":3}";
  String
      recordsByNameAndType =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"A-9883329\",\"type\":\"A\",\"data\":\"5.6.7.8\",\"ttl\":600000,\"updated\":\"2013-04-16T22:09:09.000+0000\",\"created\":\"2013-04-16T22:09:09.000+0000\"}]}";

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(records));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");
    Iterator<ResourceRecordSet<?>> records = api.iterator();

    while (records.hasNext()) {
      assertThat(records.next())
          .hasName("www.denominator.io")
          .hasTtl(600000);
    }

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records");
  }

  @Test
  public void listWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");

    assertFalse(api.iterator().hasNext());

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records");
  }

  @Test
  public void listPagesWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsPage1.replace("URL", server.url())));
    server.enqueue(new MockResponse().setBody(recordsPage2.replace("URL", server.url())));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");
    Iterator<ResourceRecordSet<?>> records = api.iterator();

    while (records.hasNext()) {
      assertThat(records.next())
          .hasName("www.denominator.io")
          .hasTtl(600000);
    }

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records?limit=3&offset=3");
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsByName));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");
    Iterator<ResourceRecordSet<?>> records = api.iterateByName("www.denominator.io");

    while (records.hasNext()) {
      assertThat(records.next())
          .hasName("www.denominator.io")
          .hasTtl(600000);
    }

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records");
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");
    assertFalse(api.iterateByName("www.denominator.io").hasNext());

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records");
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsByNameAndType));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");

    assertThat(api.getByNameAndType("www.denominator.io", "A"))
        .hasName("www.denominator.io")
        .hasType("A")
        .hasTtl(600000)
        .containsExactlyRecords(AData.create("1.2.3.4"), AData.create("5.6.7.8"));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records?name=www.denominator.io&type=A");
  }

  @Test
  public void getByNameAndType_SOA() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(soaResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");

    assertThat(api.getByNameAndType("denominator.io", "SOA"))
        .hasName("denominator.io")
        .hasType("SOA")
        .hasTtl(3601)
        .containsExactlyRecords(SOAData.builder()
                                    .mname("ns.rackspace.com")
                                    .rname("nil@denominator.io")
                                    .serial(1427817447)
                                    .refresh(3601).retry(3601)
                                    .expire(3601).minimum(3601).build());

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records?name=denominator.io&type=SOA");
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId + "");
    assertNull(api.getByNameAndType("www.denominator.io", "A"));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains/1234/records?name=www.denominator.io&type=A");
  }
}
