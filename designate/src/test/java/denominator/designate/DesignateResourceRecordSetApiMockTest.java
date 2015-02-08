package denominator.designate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import denominator.Denominator;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.rdata.MXData;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.designate.DesignateTest.aRecordResponse;
import static denominator.designate.DesignateTest.accessResponse;
import static denominator.designate.DesignateTest.domainId;
import static denominator.designate.DesignateTest.getURLReplacingQueueDispatcher;
import static denominator.designate.DesignateTest.password;
import static denominator.designate.DesignateTest.recordsResponse;
import static denominator.designate.DesignateTest.takeAuthResponse;
import static denominator.designate.DesignateTest.tenantId;
import static denominator.designate.DesignateTest.username;
import static denominator.model.ResourceRecordSets.a;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

@Test(singleThreaded = true)
public class DesignateResourceRecordSetApiMockTest {

  private static ResourceRecordSetApi mockApi(final int port) {
    return Denominator.create(new DesignateProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials(tenantId, username, password)).api().basicRecordSetsInZone(domainId);
  }

  @Test
  public void listWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      Iterator<ResourceRecordSet<?>> records = api.iterator();
      assertEquals(records.next(), ResourceRecordSet.<MXData>builder() //
          .name("denominator.io.") //
          .type("MX") //
          .ttl(300) //
          .add(MXData.create(10, "www.denominator.io.")).build());
      assertEquals(records.next(),
                   a("www.denominator.io.", 300, Arrays.asList("192.0.2.1", "192.0.2.2")));
      assertFalse(records.hasNext());

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void listWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());

      assertFalse(api.iterator().hasNext());
      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putCreatesRecord() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();

    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

      assertEquals(server.getRequestCount(), 3);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));

      RecordedRequest createRequest = server.takeRequest();
      assertEquals(createRequest.getRequestLine(),
                   format("POST /v1/domains/%s/records HTTP/1.1", domainId));
      assertEquals(new String(createRequest.getBody()), ""//
                                                        + "{\n"//
                                                        + "  \"name\": \"www.denominator.io.\",\n"//
                                                        + "  \"type\": \"A\",\n"//
                                                        + "  \"ttl\": 3600,\n"//
                                                        + "  \"data\": \"192.0.2.1\"\n"//
                                                        + "}");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putSameRecordNoOp() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", ImmutableSet.of("192.0.2.1", "192.0.2.2")));

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putUpdatesWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));
    server.enqueue(new MockResponse().setBody(aRecordResponse));
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 10000000, ImmutableSet.of("192.0.2.1", "192.0.2.2")));

      assertEquals(server.getRequestCount(), 4);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));

      RecordedRequest updateRequest = server.takeRequest();
      assertEquals(updateRequest.getRequestLine(),
                   format("PUT /v1/domains/%s/records/%s HTTP/1.1", domainId,
                          "d7eb0fc4-e069-4c92-a272-c5c969b4f558"));
      assertEquals(new String(updateRequest.getBody()), ""//
                                                        + "{\n"//
                                                        + "  \"name\": \"www.denominator.io.\",\n"//
                                                        + "  \"type\": \"A\",\n"//
                                                        + "  \"ttl\": 10000000,\n"//
                                                        + "  \"data\": \"192.0.2.1\"\n"//
                                                        + "}");

      updateRequest = server.takeRequest();
      assertEquals(updateRequest.getRequestLine(),
                   format("PUT /v1/domains/%s/records/%s HTTP/1.1", domainId,
                          "c538d70e-d65f-4d5a-92a2-cd5d4d1d9da4"));
      assertEquals(new String(updateRequest.getBody()), ""//
                                                        + "{\n"//
                                                        + "  \"name\": \"www.denominator.io.\",\n"//
                                                        + "  \"type\": \"A\",\n"//
                                                        + "  \"ttl\": 10000000,\n"//
                                                        + "  \"data\": \"192.0.2.2\"\n"//
                                                        + "}");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putOneLessDeletesExtra() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));
    server.enqueue(new MockResponse());

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", "192.0.2.2"));

      assertEquals(server.getRequestCount(), 3);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
      assertEquals(
          server.takeRequest().getRequestLine(),
          format("DELETE /v1/domains/%s/records/%s HTTP/1.1", domainId,
                 "d7eb0fc4-e069-4c92-a272-c5c969b4f558"));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iterateByNameWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertEquals(api.iterateByName("www.denominator.io.").next(),
                   a("www.denominator.io.", 300, ImmutableList.of("192.0.2.1", "192.0.2.2")));

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iterateByNameWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertFalse(api.iterateByName("www.denominator.io.").hasNext());

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertEquals(api.getByNameAndType("www.denominator.io.", "A"),
                   a("www.denominator.io.", 300, ImmutableList.of("192.0.2.1", "192.0.2.2")));

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertNull(api.getByNameAndType("www.denominator.io.", "A"));

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void deleteOne() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(recordsResponse));
    server.enqueue(new MockResponse());

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.deleteByNameAndType("denominator.io.", "MX");

      assertEquals(server.getRequestCount(), 3);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
      assertEquals(
          server.takeRequest().getRequestLine(),
          format("DELETE /v1/domains/%s/records/%s HTTP/1.1", domainId,
                 "13d2516b-1f18-455b-aa05-1997b26192ad"));
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void deleteAbsentRRSDoesNothing() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(aRecordResponse));
    server.enqueue(new MockResponse());

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.deleteByNameAndType("www1.denominator.io.", "A");

      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(),
                   format("GET /v1/domains/%s/records HTTP/1.1", domainId));
    } finally {
      server.shutdown();
    }
  }
}
