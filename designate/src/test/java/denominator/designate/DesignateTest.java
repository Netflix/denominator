package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import denominator.Credentials;
import denominator.Provider;
import denominator.designate.Designate.Record;
import denominator.designate.KeystoneV2.TokenIdAndPublicURL;
import denominator.model.Zone;
import feign.Feign;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class DesignateTest {

  @Rule
  public MockDesignateServer server = new MockDesignateServer();

  @Test
  public void authSuccess() throws Exception {
    server.credentials("tenantId", "username", "password");
    server.enqueueAuthResponse();

    DesignateProvider.FeignModule module = new DesignateProvider.FeignModule();
    KeystoneV2 api = module.keystoneV2(module.feign(module.logger(), module.logLevel()));
    TokenIdAndPublicURL tokenIdAndPublicURL = api.passwordAuth(
        URI.create(server.url()), "tenantId", "username", "password");

    assertThat(tokenIdAndPublicURL.tokenId).isEqualTo(server.tokenId());
    assertThat(tokenIdAndPublicURL.publicURL).isEqualTo(server.url() + "/v1");

    server.assertAuthRequest();
  }

  @Test
  public void limitsSuccess() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{\n"
                                              + "  \"limits\": {\n"
                                              + "    \"absolute\": {\n"
                                              + "      \"maxDomains\": 20,\n"
                                              + "      \"maxDomainRecords\": 5000\n"
                                              + "    }\n"
                                              + "  }\n"
                                              + "}"));

    assertThat(mockApi().limits()).hasSize(1);

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1/limits");
  }

  @Test
  public void domainsPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    assertThat(mockApi().domains()).containsExactly(
        Zone.create(domainId, "denominator.io.", 3601, "nil@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1/domains");
  }

  @Test
  public void domainsAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    assertThat(mockApi().domains()).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1/domains");
  }

  @Test
  public void recordsPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));

    // note that the response parser orders these by name, type, data!
    List<Record> records = mockApi().records(domainId);
    assertThat(records).extracting("id")
        .containsExactly(
            "13d2516b-1f18-455b-aa05-1997b26192ad",
            "d7eb0fc4-e069-4c92-a272-c5c969b4f558",
            "c538d70e-d65f-4d5a-92a2-cd5d4d1d9da4"
        );
    assertThat(records).extracting("name", "type", "ttl", "priority", "data")
        .containsExactly(
            tuple("denominator.io.", "MX", 300, 10, "www.denominator.io."),
            tuple("www.denominator.io.", "A", 300, null, "192.0.2.1"),
            tuple("www.denominator.io.", "A", 300, null, "192.0.2.2")
        );

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void recordsAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));

    assertThat(mockApi().records(domainId)).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1/domains/%s/records", domainId));
  }

  @Test
  public void createMXRecord() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordResponse));

    Record record = new Record();
    record.name = "denominator.io.";
    record.data = "www.denominator.io.";
    record.priority = 10;
    record.ttl = 300;
    record.type = "MX";

    List<Record> records = Arrays.asList(mockApi().createRecord(domainId, record));
    assertThat(records).extracting("id")
        .containsExactly("13d2516b-1f18-455b-aa05-1997b26192ad");
    assertThat(records).extracting("name", "type", "ttl", "priority", "data")
        .containsExactly(tuple("denominator.io.", "MX", 300, 10, "www.denominator.io."));

    server.assertAuthRequest();
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
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordResponse));

    Record record = new Record();
    record.id = "13d2516b-1f18-455b-aa05-1997b26192ad";
    record.name = "denominator.io.";
    record.data = "www.denominator.io.";
    record.priority = 10;
    record.ttl = 300;
    record.type = "MX";

    List<Record> records = Arrays.asList(mockApi().updateRecord(domainId, record.id, record));
    assertThat(records).extracting("id")
        .containsExactly("13d2516b-1f18-455b-aa05-1997b26192ad");
    assertThat(records).extracting("name", "type", "ttl", "priority", "data")
        .containsExactly(tuple("denominator.io.", "MX", 300, 10, "www.denominator.io."));

    server.assertAuthRequest();
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
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    Record record = new Record();
    record.name = "www.denominator.io.";
    record.data = "192.0.2.1";
    record.type = "A";

    List<Record> records = Arrays.asList(mockApi().createRecord(domainId, record));
    assertThat(records).extracting("id")
        .containsExactly("13d2516b-1f18-455b-aa05-1997b26192ad");
    assertThat(records).extracting("name", "type", "ttl", "priority", "data")
        .containsExactly(tuple("www.denominator.io.", "A", null, null, "192.0.2.1"));

    server.assertAuthRequest();
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
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(aRecordResponse));

    Record record = new Record();
    record.id = "13d2516b-1f18-455b-aa05-1997b26192ad";
    record.name = "www.denominator.io.";
    record.data = "192.0.2.1";
    record.type = "A";

    List<Record> records = Arrays.asList(mockApi().updateRecord(domainId, record.id, record));
    assertThat(records).extracting("id")
        .containsExactly("13d2516b-1f18-455b-aa05-1997b26192ad");
    assertThat(records).extracting("name", "type", "ttl", "priority", "data")
        .containsExactly(tuple("www.denominator.io.", "A", null, null, "192.0.2.1"));

    server.assertAuthRequest();
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
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse());

    String recordId = "13d2516b-1f18-455b-aa05-1997b26192ad";

    mockApi().deleteRecord(domainId, recordId);

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath(format("/v1/domains/%s/records/%s", domainId, recordId));
  }

  Designate mockApi() {
    DesignateProvider.FeignModule module = new DesignateProvider.FeignModule();
    Feign feign = module.feign(module.logger(), module.logLevel());
    KeystoneV2 keystoneV2 = module.keystoneV2(feign);
    Provider provider = new DesignateProvider() {
      @Override
      public String url() {
        return server.url();
      }
    };
    javax.inject.Provider<Credentials> credentials = new javax.inject.Provider<Credentials>() {

      @Override
      public Credentials get() {
        return server.credentials();
      }

    };
    return feign.newInstance(
        new DesignateTarget(provider,
                            new InvalidatableAuthProvider(provider, keystoneV2, credentials)));
  }

  static String domainId = "62ac4caf-6108-4b74-b6fe-460967db32a7";
  static String domainResponse = "{\n"
                         + "  \"created_at\": \"2015-04-05T15:48:53.271013\",\n"
                         + "  \"description\": null,\n"
                         + "  \"email\": \"nil@denominator.io\",\n"
                         + format("  \"id\": \"%s\",\n", domainId)
                         + "  \"name\": \"denominator.io.\",\n"
                         + "  \"serial\": 1428248933,\n"
                         + "  \"ttl\": 3601,\n"
                         + "  \"updated_at\": null\n"
                         + "}\n";
  static String domainsResponse = "{\n"
                                  + "  \"domains\": [\n"
                                  + domainResponse
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
}
