package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import denominator.designate.Designate.Record;
import denominator.designate.KeystoneV2.TokenIdAndPublicURL;
import denominator.model.Zone;
import feign.Feign;

import static feign.Target.EmptyTarget.create;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class DesignateTest {

  MockDesignateServer server;

  static String domainId = "62ac4caf-6108-4b74-b6fe-460967db32a7";
  static String domainsResponse = "{\n"
                                  + "  \"domains\": [\n"
                                  + "    {\n"
                                  + "      \"name\": \"denominator.io.\",\n"
                                  + "      \"created_at\": \"2013-07-07T17:55:21.000000\",\n"
                                  + "      \"updated_at\": null,\n"
                                  + "      \"email\": \"admin@denominator.io\",\n"
                                  + "      \"ttl\": 3600,\n"
                                  + "      \"serial\": 1373219721,\n"
                                  + format("      \"id\": \"%s\"\n", domainId)
                                  + "    }\n"
                                  + "  ]\n"
                                  + "}";
  // NOTE records are allowed to be out of order by type
  static String recordsResponse = "{\n"
                                  + "  \"records\": [\n"
                                  + "    {\n"
                                  + "      \"name\": \"www.denominator.io.\",\n"
                                  + "      \"data\": \"192.0.2.2\",\n"
                                  + "      \"created_at\": \"2013-07-07T18:09:47.000000\",\n"
                                  + "      \"updated_at\": null,\n"
                                  + "      \"id\": \"c538d70e-d65f-4d5a-92a2-cd5d4d1d9da4\",\n"
                                  + "      \"priority\": null,\n"
                                  + "      \"ttl\": 300,\n"
                                  + "      \"type\": \"A\",\n"
                                  + format("      \"domain_id\": \"%s\"\n", domainId)
                                  + "    },\n"
                                  + "    {\n"
                                  + "      \"name\": \"denominator.io.\",\n"
                                  + "      \"data\": \"www.denominator.io.\",\n"
                                  + "      \"created_at\": \"2013-07-07T18:09:47.000000\",\n"
                                  + "      \"updated_at\": null,\n"
                                  + "      \"id\": \"13d2516b-1f18-455b-aa05-1997b26192ad\",\n"
                                  + "      \"priority\": 10,\n"
                                  + "      \"ttl\": 300,\n"
                                  + "      \"type\": \"MX\",\n"
                                  + format("      \"domain_id\": \"%s\"\n", domainId)
                                  + "    },\n"
                                  + "    {\n"
                                  + "      \"name\": \"www.denominator.io.\",\n"
                                  + "      \"data\": \"192.0.2.1\",\n"
                                  + "      \"created_at\": \"2013-07-07T18:09:31.000000\",\n"
                                  + "      \"updated_at\": null,\n"
                                  + "      \"id\": \"d7eb0fc4-e069-4c92-a272-c5c969b4f558\",\n"
                                  + "      \"priority\": null,\n"
                                  + "      \"ttl\": 300,\n"
                                  + "      \"type\": \"A\",\n"
                                  + format("      \"domain_id\": \"%s\"\n", domainId)
                                  + "    }\n"
                                  + "  ]\n"
                                  + "}";
  static String mxRecordResponse = "{\n"
                                   + "  \"name\": \"denominator.io.\",\n"
                                   + "  \"data\": \"www.denominator.io.\",\n"
                                   + "  \"created_at\": \"2013-07-07T18:09:47.000000\",\n"
                                   + "  \"updated_at\": null,\n"
                                   + "  \"id\": \"13d2516b-1f18-455b-aa05-1997b26192ad\",\n"
                                   + "  \"priority\": 10,\n"
                                   + "  \"ttl\": 300,\n"
                                   + "  \"type\": \"MX\",\n"
                                   + format("  \"domain_id\": \"%s\"\n", domainId)
                                   + "}";
  static String aRecordResponse = "{\n"
                                  + "  \"name\": \"www.denominator.io.\",\n"
                                  + "  \"data\": \"192.0.2.1\",\n"
                                  + "  \"created_at\": \"2013-07-07T18:09:47.000000\",\n"
                                  + "  \"updated_at\": null,\n"
                                  + "  \"id\": \"13d2516b-1f18-455b-aa05-1997b26192ad\",\n"
                                  + "  \"ttl\": null,\n"
                                  + "  \"type\": \"A\",\n"
                                  + format("  \"domain_id\": \"%s\"\n", domainId)
                                  + "}";

  @Test
  public void authSuccess() throws IOException, InterruptedException, URISyntaxException {
    server.credentials("tenantId", "username", "password");
    server.enqueueAuthResponse();

    KeystoneV2 api = Feign.create(create(KeystoneV2.class), new DesignateProvider.FeignModule());
    TokenIdAndPublicURL tokenIdAndPublicURL = api.passwordAuth(
        URI.create(server.url()), "tenantId", "username", "password");

    assertEquals(tokenIdAndPublicURL.tokenId, server.tokenId());
    assertEquals(tokenIdAndPublicURL.publicURL, server.url() + "/v1");

    server.assertAuthRequest();
  }

  @Test
  public void limitsSuccess() throws Exception {
    server.enqueue(new MockResponse().setBody("{\n"
                                              + "  \"limits\": {\n"
                                              + "    \"absolute\": {\n"
                                              + "      \"maxDomains\": 20,\n"
                                              + "      \"maxDomainRecords\": 5000\n"
                                              + "    }\n"
                                              + "  }\n"
                                              + "}"));

    assertFalse(mockApi().limits().isEmpty());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1/limits");
  }

  @Test
  public void domainsPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(domainsResponse));

    assertThat(mockApi().domains())
        .containsExactly(Zone.create("denominator.io.", domainId));

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1/domains");
  }

  @Test
  public void domainsAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    assertTrue(mockApi().domains().isEmpty());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1/domains");
  }

  @Test
  public void recordsPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(recordsResponse));

    List<Record> records = mockApi().records(domainId);
    assertEquals(records.size(), 3);

    // note that the response parser orders these by name, type, data!

    assertEquals(records.get(0).id, "13d2516b-1f18-455b-aa05-1997b26192ad");
    assertEquals(records.get(0).name, "denominator.io.");
    assertEquals(records.get(0).data, "www.denominator.io.");
    assertEquals(records.get(0).priority.intValue(), 10);
    assertEquals(records.get(0).ttl.intValue(), 300);
    assertEquals(records.get(0).type, "MX");

    assertEquals(records.get(1).id, "d7eb0fc4-e069-4c92-a272-c5c969b4f558");
    assertEquals(records.get(1).name, "www.denominator.io.");
    assertEquals(records.get(1).data, "192.0.2.1");
    assertNull(records.get(1).priority);
    assertEquals(records.get(1).ttl.intValue(), 300);
    assertEquals(records.get(1).type, "A");

    assertEquals(records.get(2).id, "c538d70e-d65f-4d5a-92a2-cd5d4d1d9da4");
    assertEquals(records.get(2).name, "www.denominator.io.");
    assertEquals(records.get(2).data, "192.0.2.2");
    assertNull(records.get(2).priority);
    assertEquals(records.get(2).ttl.intValue(), 300);
    assertEquals(records.get(2).type, "A");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void recordsAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    assertTrue(mockApi().records(domainId).isEmpty());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void createMXRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordResponse));

    Record record = new Record();
    record.name = "denominator.io.";
    record.data = "www.denominator.io.";
    record.priority = 10;
    record.ttl = 300;
    record.type = "MX";

    record = mockApi().createRecord(domainId, record);

    assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
    assertEquals(record.name, "denominator.io.");
    assertEquals(record.data, "www.denominator.io.");
    assertEquals(record.priority.intValue(), 10);
    assertEquals(record.ttl.intValue(), 300);
    assertEquals(record.type, "MX");

    server.assertRequest()
        .hasMethod("POST")
        .hasPath(format("/v1/domains/%s/records", domainId))
        .hasBody("{\n"
                 + "  \"name\": \"denominator.io.\",\n"
                 + "  \"type\": \"MX\",\n"
                 + "  \"ttl\": 300,\n"
                 + "  \"data\": \"www.denominator.io.\",\n"
                 + "  \"priority\": 10\n"
                 + "}");
  }

  @Test
  public void updateMXRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordResponse));

    Record record = new Record();
    record.id = "13d2516b-1f18-455b-aa05-1997b26192ad";
    record.name = "denominator.io.";
    record.data = "www.denominator.io.";
    record.priority = 10;
    record.ttl = 300;
    record.type = "MX";

    record = mockApi().updateRecord(domainId, record.id, record);

    assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
    assertEquals(record.name, "denominator.io.");
    assertEquals(record.data, "www.denominator.io.");
    assertEquals(record.priority.intValue(), 10);
    assertEquals(record.ttl.intValue(), 300);
    assertEquals(record.type, "MX");

    server.assertRequest()
        .hasMethod("PUT")
        .hasPath(format("/v1/domains/%s/records/%s", domainId, record.id))
        .hasBody("{\n"
                 + "  \"name\": \"denominator.io.\",\n"
                 + "  \"type\": \"MX\",\n"
                 + "  \"ttl\": 300,\n"
                 + "  \"data\": \"www.denominator.io.\",\n"
                 + "  \"priority\": 10\n"
                 + "}");
  }

  @Test
  public void createARecord() throws Exception {
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    Record record = new Record();
    record.name = "www.denominator.io.";
    record.data = "192.0.2.1";
    record.type = "A";

    record = mockApi().createRecord(domainId, record);

    assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
    assertEquals(record.name, "www.denominator.io.");
    assertEquals(record.data, "192.0.2.1");
    assertNull(record.priority);
    assertNull(record.ttl);
    assertEquals(record.type, "A");

    server.assertRequest()
        .hasMethod("POST")
        .hasPath(format("/v1/domains/%s/records", domainId))
        .hasBody("{\n"
                 + "  \"name\": \"www.denominator.io.\",\n"
                 + "  \"type\": \"A\",\n"
                 + "  \"data\": \"192.0.2.1\"\n"
                 + "}");
  }

  @Test
  public void updateARecord() throws Exception {
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    Record record = new Record();
    record.id = "13d2516b-1f18-455b-aa05-1997b26192ad";
    record.name = "www.denominator.io.";
    record.data = "192.0.2.1";
    record.type = "A";

    record = mockApi().updateRecord(domainId, record.id, record);

    assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
    assertEquals(record.name, "www.denominator.io.");
    assertEquals(record.data, "192.0.2.1");
    assertNull(record.priority);
    assertNull(record.ttl);
    assertEquals(record.type, "A");

    server.assertRequest()
        .hasMethod("PUT")
        .hasPath(format("/v1/domains/%s/records/%s", domainId, record.id))
        .hasBody("{\n"
                 + "  \"name\": \"www.denominator.io.\",\n"
                 + "  \"type\": \"A\",\n"
                 + "  \"data\": \"192.0.2.1\"\n"
                 + "}");
  }

  @Test
  public void deleteRecord() throws Exception {
    server.enqueue(new MockResponse());

    String recordId = "13d2516b-1f18-455b-aa05-1997b26192ad";

    mockApi().deleteRecord(domainId, recordId);

    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath(format("/v1/domains/%s/records/%s", domainId, recordId));
  }

  Designate mockApi() {
    final TokenIdAndPublicURL tokenIdAndPublicURL = new TokenIdAndPublicURL();
    tokenIdAndPublicURL.tokenId = server.tokenId();
    tokenIdAndPublicURL.publicURL = server.url() + "/v1";
    return Feign.create(new DesignateTarget(new DesignateProvider() {
      @Override
      public String url() {
        return tokenIdAndPublicURL.publicURL;
      }
    }, new javax.inject.Provider<TokenIdAndPublicURL>() {

      @Override
      public TokenIdAndPublicURL get() {
        return tokenIdAndPublicURL;
      }

    }), new DesignateProvider.FeignModule());
  }


  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDesignateServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
