package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;
import denominator.model.rdata.MXData;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.designate.DesignateTest.aRecordResponse;
import static denominator.designate.DesignateTest.domainId;
import static denominator.designate.DesignateTest.recordsResponse;
import static denominator.model.ResourceRecordSets.a;
import static java.lang.String.format;

public class DesignateResourceRecordSetApiMockTest {

  @Rule
  public MockDesignateServer server = new MockDesignateServer();

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));

    Iterator<ResourceRecordSet<?>>
        records =
        server.connect().api().basicRecordSetsInZone(domainId).iterator();
    assertThat(records.next())
        .hasName("denominator.io.")
        .hasType("MX")
        .hasTtl(300)
        .containsExactlyRecords(MXData.create(10, "www.denominator.io."));

    assertThat(records.next())
        .hasName("www.denominator.io.")
        .hasType("A")
        .hasTtl(300)
        .containsExactlyRecords(AData.create("192.0.2.1"), AData.create("192.0.2.2"));

    assertThat(records).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void listWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    assertThat(server.connect().api().basicRecordSetsInZone(domainId)).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void putCreatesRecord() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
    server.assertRequest()
        .hasMethod("POST")
        .hasPath(format("/v1/domains/%s/records", domainId))
        .hasBody("{\n"
                 + "  \"name\": \"www.denominator.io.\",\n"
                 + "  \"type\": \"A\",\n"
                 + "  \"ttl\": 3600,\n"
                 + "  \"data\": \"192.0.2.1\"\n"
                 + "}");
  }

  @Test
  public void putSameRecordNoOp() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    api.put(a("www.denominator.io.", Arrays.asList("192.0.2.1", "192.0.2.2")));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void putUpdatesWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));
    server.enqueue(new MockResponse().setBody(aRecordResponse));
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    api.put(a("www.denominator.io.", 10000000, Arrays.asList("192.0.2.1", "192.0.2.2")));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath(format("/v1/domains/%s/records/%s", domainId,
                        "d7eb0fc4-e069-4c92-a272-c5c969b4f558"))
        .hasBody("{\n"
                 + "  \"name\": \"www.denominator.io.\",\n"
                 + "  \"type\": \"A\",\n"
                 + "  \"ttl\": 10000000,\n"
                 + "  \"data\": \"192.0.2.1\"\n"
                 + "}");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath(format("/v1/domains/%s/records/%s", domainId,
                        "c538d70e-d65f-4d5a-92a2-cd5d4d1d9da4"))
        .hasBody("{\n"
                 + "  \"name\": \"www.denominator.io.\",\n"
                 + "  \"type\": \"A\",\n"
                 + "  \"ttl\": 10000000,\n"
                 + "  \"data\": \"192.0.2.2\"\n"
                 + "}");
  }

  @Test
  public void putOneLessDeletesExtra() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));
    server.enqueue(new MockResponse());

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    api.put(a("www.denominator.io.", "192.0.2.2"));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath(format("/v1/domains/%s/records/%s", domainId,
                        "d7eb0fc4-e069-4c92-a272-c5c969b4f558"));
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);

    assertThat(api.iterateByName("www.denominator.io.").next())
        .hasName("www.denominator.io.")
        .hasType("A")
        .hasTtl(300)
        .containsExactlyRecords(AData.create("192.0.2.1"), AData.create("192.0.2.2"));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    assertThat(api.iterateByName("www.denominator.io.")).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);

    assertThat(api.getByNameAndType("www.denominator.io.", "A"))
        .hasName("www.denominator.io.")
        .hasType("A")
        .hasTtl(300)
        .containsExactlyRecords(AData.create("192.0.2.1"), AData.create("192.0.2.2"));

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    assertThat(api.getByNameAndType("www.denominator.io.", "A")).isNull();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void deleteOne() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));
    server.enqueue(new MockResponse());

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    api.deleteByNameAndType("denominator.io.", "MX");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath(format("/v1/domains/%s/records/%s", domainId,
                        "13d2516b-1f18-455b-aa05-1997b26192ad"));
  }

  @Test
  public void deleteAbsentRRSDoesNothing() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(aRecordResponse));
    server.enqueue(new MockResponse());

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone(domainId);
    api.deleteByNameAndType("www1.denominator.io.", "A");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }
}
