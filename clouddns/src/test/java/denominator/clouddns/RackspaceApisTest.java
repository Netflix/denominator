package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.net.URI;

import denominator.Credentials;
import denominator.Provider;
import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.CloudIdentity;
import denominator.clouddns.RackspaceApis.Job;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import denominator.model.Zone;
import feign.Feign;

import static denominator.assertj.ModelAssertions.assertThat;
import static java.lang.String.format;
import static org.assertj.core.groups.Tuple.tuple;

public class RackspaceApisTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

  @Test
  public void passwordAuth() throws Exception {
    server.credentials("username", "password");
    server.enqueueAuthResponse();

    CloudDNSProvider.FeignModule module = new CloudDNSProvider.FeignModule();
    CloudIdentity api = module.cloudIdentity(module.feign(module.logger(), module.logLevel()));

    TokenIdAndPublicURL tokenIdAndPublicURL = api.passwordAuth(
        URI.create(server.url()), "username", "password");

    assertThat(tokenIdAndPublicURL.tokenId).isEqualTo(server.tokenId());
    assertThat(tokenIdAndPublicURL.publicURL)
        .isEqualTo(server.url() + "/v1.0/" + server.tenantId());

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/tokens")
        .hasBody(
            "{\"auth\":{\"passwordCredentials\":{\"username\":\"username\",\"password\":\"password\"}}}");
  }

  @Test
  public void limitsSuccess() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(limitsResponse));

    assertThat(mockApi().limits()).isNotEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/limits");
  }

  @Test
  public void domainsPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    assertThat(mockApi().domains().get(0))
        .hasName("denominator.io");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains");
  }

  /**
   * Rackspace doesn't expose the ttl in domain list. A dummy TTL of zero is added as this result is
   * never used directly.
   */
  @Test
  public void domainsByNamePresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    assertThat(mockApi().domainsByName("denominator.io")).containsExactly(
        Zone.create("1234", "denominator.io", 0, "nil@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains?name=denominator.io");
  }

  @Test
  public void domainsAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{\"domains\":[]}"));

    assertThat(mockApi().domains()).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains");
  }

  @Test
  public void recordsPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(recordsResponse));

    assertThat(mockApi().records(domainId))
        .extracting("id", "name", "type", "ttl", "priority", "data")
        .containsExactly(
            tuple("A-10465369", "www.denominator.io", "A", 300, null, "192.0.2.1"),
            tuple("A-10465370", "www.denominator.io", "A", 300, null, "192.0.2.2"),
            tuple("NS-9084762", "www.denominator.io", "NS", 300, null, "dns1.stabletransit.com"),
            tuple("NS-9084763", "www.denominator.io", "NS", 300, null, "dns2.stabletransit.com")
        );

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1.0/123123/domains/%s/records", domainId));
  }

  @Test
  public void recordsAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{\"records\":[]}"));

    assertThat(mockApi().records(domainId)).isEmpty();

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1.0/123123/domains/%s/records", domainId));
  }

  @Test
  public void createMXRecord() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordInitialResponse));

    Job job = mockApi().createRecordWithPriority(domainId, "www.denominator.io", "MX",
                                                 1800, "mail.denominator.io", 10);

    assertThat(job.id).isEqualTo("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
    assertThat(job.status).isEqualTo("RUNNING");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath(format("/v1.0/123123/domains/%s/records", domainId))
        .hasBody(mxRecordCreateRequest);
  }

  @Test
  public void runningRequest() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordRunningResponse));

    Job job = mockApi().getStatus("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");

    assertThat(job.id).isEqualTo("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
    assertThat(job.status).isEqualTo("RUNNING");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3?showDetails=true");
  }

  @Test
  public void completedRequest() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordCompletedResponse));

    Job job = mockApi().getStatus("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");

    assertThat(job.id).isEqualTo("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
    assertThat(job.status).isEqualTo("COMPLETED");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3?showDetails=true");
  }

  @Test
  public void updateMXRecord() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordUpdateInitialResponse));

    Job job = mockApi().updateRecord(domainId, "MX-4582544", 600, "mail.denominator.io");

    assertThat(job.id).isEqualTo("e32eace1-c44f-49af-8f74-768fa34469f4");
    assertThat(job.status).isEqualTo("RUNNING");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath(format("/v1.0/123123/domains/%s/records/MX-4582544", domainId))
        .hasBody(mxRecordUpdateRequest);
  }

  @Test
  public void deleteRecord() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(mxRecordDeleteInitialResponse));

    Job job = mockApi().deleteRecord(domainId, "MX-4582544");

    assertThat(job.id).isEqualTo("da520d24-dd5b-4387-92be-2020a7f2b176");
    assertThat(job.status).isEqualTo("RUNNING");

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath(format("/v1.0/123123/domains/%s/records/MX-4582544", domainId));
  }

  CloudDNS mockApi() {
    CloudDNSProvider.FeignModule module = new CloudDNSProvider.FeignModule();
    Feign feign = module.feign(module.logger(), module.logLevel());
    CloudIdentity cloudIdentity = module.cloudIdentity(feign);
    Provider provider = new CloudDNSProvider() {
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
        new CloudDNSTarget(provider,
                           new InvalidatableAuthProvider(provider, cloudIdentity, credentials)));
  }

  static String limitsResponse = "{\n"
                                 + "  \"rates\" : {\n"
                                 + "    \"rate\" : [ {\n"
                                 + "      \"uri\" : \"*/status/*\",\n"
                                 + "      \"limit\" : [ {\n"
                                 + "        \"value\" : 5,\n"
                                 + "        \"verb\" : \"GET\",\n"
                                 + "        \"unit\" : \"SECOND\"\n"
                                 + "      } ],\n"
                                 + "      \"regex\" : \".*/v\\\\d+\\\\.\\\\d+/(\\\\d+/status).*\"\n"
                                 + "    }, {\n"
                                 + "      \"uri\" : \"*/domains*\",\n"
                                 + "      \"limit\" : [ {\n"
                                 + "        \"value\" : 100,\n"
                                 + "        \"verb\" : \"GET\",\n"
                                 + "        \"unit\" : \"MINUTE\"\n"
                                 + "      }, {\n"
                                 + "        \"value\" : 25,\n"
                                 + "        \"verb\" : \"POST\",\n"
                                 + "        \"unit\" : \"MINUTE\"\n"
                                 + "      }, {\n"
                                 + "        \"value\" : 50,\n"
                                 + "        \"verb\" : \"PUT\",\n"
                                 + "        \"unit\" : \"MINUTE\"\n"
                                 + "      }, {\n"
                                 + "        \"value\" : 50,\n"
                                 + "        \"verb\" : \"DELETE\",\n"
                                 + "        \"unit\" : \"MINUTE\"\n"
                                 + "      } ],\n"
                                 + "      \"regex\" : \".*/v\\\\d+\\\\.\\\\d+/(\\\\d+/domains).*\"\n"
                                 + "    } ]\n"
                                 + "  }\n"
                                 + "}";

  static int domainId = 1234;
  static String
      domainsResponse =
      "{\"domains\":[{\"name\":\"denominator.io\",\"id\":1234,\"accountId\":123123,\"emailAddress\":\"nil@denominator.io\",\"updated\":\"2013-09-02T19:46:56.000+0000\",\"created\":\"2013-09-02T19:45:51.000+0000\"}],\"totalEntries\":1}";
  static String
      soaResponse =
      "{\"records\":[{\"name\":\"denominator.io\",\"id\":\"SOA-4612221\",\"type\":\"SOA\",\"data\":\"ns.rackspace.com nil@denominator.io 1427817447\",\"ttl\":3601}]}";
  // NOTE records are allowed to be out of order by type
  static String
      recordsResponse =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-10465369\",\"type\":\"A\",\"data\":\"192.0.2.1\",\"ttl\":300,\"updated\":\"2013-09-02T20:51:55.000+0000\",\"created\":\"2013-09-02T20:51:55.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"A-10465370\",\"type\":\"A\",\"data\":\"192.0.2.2\",\"ttl\":300,\"updated\":\"2013-09-02T20:52:07.000+0000\",\"created\":\"2013-09-02T20:52:07.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-9084762\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":300,\"updated\":\"2013-09-02T20:51:12.000+0000\",\"created\":\"2013-09-02T20:51:12.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-9084763\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":300,\"updated\":\"2013-09-02T20:51:12.000+0000\",\"created\":\"2013-09-02T20:51:12.000+0000\"}],\"totalEntries\":4}";
  static String
      mxRecordCreateRequest =
      "{\"records\":[{\"name\":\"www.denominator.io\",\"type\":\"MX\",\"ttl\":\"1800\",\"data\":\"mail.denominator.io\",\"priority\":\"10\"}]}";
  static String
      mxRecordInitialResponse =
      "{\"request\":\"{\\\"records\\\":[{\\\"name\\\":\\\"www.denominator.io\\\",\\\"type\\\":\\\"MX\\\",\\\"ttl\\\":\\\"1800\\\",\\\"data\\\":\\\"mail.denominator.io\\\",\\\"priority\\\":\\\"10\\\"}]}\",\"status\":\"RUNNING\",\"verb\":\"POST\",\"jobId\":\"0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records\"}";
  static String
      mxRecordRunningResponse =
      "{\"request\":\"{\\\"records\\\":[{\\\"name\\\":\\\"www.denominator.io\\\",\\\"type\\\":\\\"MX\\\",\\\"ttl\\\":\\\"1800\\\",\\\"data\\\":\\\"mail.denominator.io\\\",\\\"priority\\\":\\\"10\\\"}]}\",\"status\":\"RUNNING\",\"verb\":\"POST\",\"jobId\":\"0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records\"}";
  static String
      mxRecordCompletedResponse =
      "{\"request\":\"{\\\"records\\\":[{\\\"name\\\":\\\"www.denominator.io\\\",\\\"type\\\":\\\"MX\\\",\\\"ttl\\\":\\\"1800\\\",\\\"data\\\":\\\"mail.denominator.io\\\",\\\"priority\\\":\\\"10\\\"}]}\",\"response\":{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"MX-4582544\",\"priority\":10,\"type\":\"MX\",\"data\":\"mail.denominator.io\",\"ttl\":1800,\"updated\":\"2013-09-02T21:10:03.000+0000\",\"created\":\"2013-09-02T21:10:03.000+0000\"}]},\"status\":\"COMPLETED\",\"verb\":\"POST\",\"jobId\":\"0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records\"}";
  static String mxRecordUpdateRequest = "{\"ttl\":\"600\",\"data\":\"mail.denominator.io\"}";
  static String
      mxRecordUpdateInitialResponse =
      "{\"request\":\"{\\\"ttl\\\":\\\"600\\\",\\\"data\\\":\\\"mail.denominator.io\\\"}\",\"status\":\"RUNNING\",\"verb\":\"PUT\",\"jobId\":\"e32eace1-c44f-49af-8f74-768fa34469f4\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/e32eace1-c44f-49af-8f74-768fa34469f4\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";
  static String
      mxRecordDeleteInitialResponse =
      "{\"status\":\"RUNNING\",\"verb\":\"DELETE\",\"jobId\":\"da520d24-dd5b-4387-92be-2020a7f2b176\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/da520d24-dd5b-4387-92be-2020a7f2b176\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";
}
