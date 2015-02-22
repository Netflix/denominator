package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.CloudIdentity;
import denominator.clouddns.RackspaceApis.JobIdAndStatus;
import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Record;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import denominator.model.Zone;
import feign.Feign;

import static denominator.assertj.ModelAssertions.assertThat;
import static feign.Target.EmptyTarget.create;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class RackspaceApisTest {

  MockCloudDNSServer server;

  @Test
  public void passwordAuth() throws Exception {
    server.credentials("username", "password");
    server.enqueueAuthResponse();

    CloudIdentity
        api =
        Feign.create(create(CloudIdentity.class), new CloudDNSProvider.FeignModule());
    TokenIdAndPublicURL tokenIdAndPublicURL = api.passwordAuth(
        URI.create(server.url()), "username", "password");

    assertEquals(tokenIdAndPublicURL.tokenId, server.tokenId());
    assertEquals(tokenIdAndPublicURL.publicURL, server.url() + "/v1.0/" + server.tenantId());

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/tokens")
        .hasBody(
            "{\"auth\":{\"passwordCredentials\":{\"username\":\"username\",\"password\":\"password\"}}}");
  }

  @Test
  public void limitsSuccess() throws Exception {
    server.enqueue(new MockResponse().setBody(limitsResponse));

    assertFalse(mockApi().limits().isEmpty());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/limits");
  }

  @Test
  public void domainsPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(domainsResponse));

    assertThat(mockApi().domains().get(0))
        .hasName("denominator.io")
        .hasId(String.valueOf("1234"));

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains");
  }

  @Test
  public void domainsAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"domains\":[]}"));

    assertTrue(mockApi().domains().isEmpty());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/domains");
  }

  @Test
  public void recordsPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(recordsResponse));

    List<Record> records = mockApi().records(domainId);
    assertEquals(records.size(), 4);

    assertEquals(records.get(0).id, "A-10465369");
    assertEquals(records.get(0).name, "www.denominator.io");
    assertEquals(records.get(0).data(), "192.0.2.1");
    assertNull(records.get(0).priority);
    assertEquals(records.get(0).ttl.intValue(), 300);
    assertEquals(records.get(0).type, "A");

    assertEquals(records.get(1).id, "A-10465370");
    assertEquals(records.get(1).name, "www.denominator.io");
    assertEquals(records.get(1).data(), "192.0.2.2");
    assertNull(records.get(1).priority);
    assertEquals(records.get(1).ttl.intValue(), 300);
    assertEquals(records.get(1).type, "A");

    assertEquals(records.get(2).id, "NS-9084762");
    assertEquals(records.get(2).name, "www.denominator.io");
    assertEquals(records.get(2).data(), "dns1.stabletransit.com");
    assertEquals(records.get(2).ttl.intValue(), 300);
    assertEquals(records.get(2).type, "NS");

    assertEquals(records.get(3).id, "NS-9084763");
    assertEquals(records.get(3).name, "www.denominator.io");
    assertEquals(records.get(3).data(), "dns2.stabletransit.com");
    assertEquals(records.get(3).ttl.intValue(), 300);
    assertEquals(records.get(3).type, "NS");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1.0/123123/domains/%s/records", domainId));
  }

  @Test
  public void recordsAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"records\":[]}"));

    assertTrue(mockApi().records(domainId).isEmpty());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath(format("/v1.0/123123/domains/%s/records", domainId));
  }

  @Test
  public void createMXRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordInitialResponse));

    JobIdAndStatus job = mockApi().createRecordWithPriority(domainId, "www.denominator.io", "MX",
                                                            1800,
                                                            "mail.denominator.io", 10);

    assertEquals(job.id, "0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
    assertEquals(job.status, "RUNNING");

    server.assertRequest()
        .hasMethod("POST")
        .hasPath(format("/v1.0/123123/domains/%s/records", domainId))
        .hasBody(mxRecordCreateRequest);
  }

  @Test
  public void runningRequest() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordRunningResponse));

    JobIdAndStatus job = mockApi().getStatus("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");

    assertEquals(job.id, "0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
    assertEquals(job.status, "RUNNING");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3?showDetails=true");
  }

  @Test
  public void completedRequest() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordCompletedResponse));

    JobIdAndStatus job = mockApi().getStatus("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");

    assertEquals(job.id, "0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
    assertEquals(job.status, "COMPLETED");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3?showDetails=true");
  }

  @Test
  public void updateMXRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordUpdateInitialResponse));

    JobIdAndStatus job = mockApi().updateRecord(domainId, "MX-4582544", 600,
                                                "mail.denominator.io");

    assertEquals(job.id, "e32eace1-c44f-49af-8f74-768fa34469f4");
    assertEquals(job.status, "RUNNING");

    server.assertRequest()
        .hasMethod("PUT")
        .hasPath(format("/v1.0/123123/domains/%s/records/MX-4582544", domainId))
        .hasBody(mxRecordUpdateRequest);
  }

  @Test
  public void deleteRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(mxRecordDeleteInitialResponse));

    JobIdAndStatus job = mockApi().deleteRecord(domainId, "MX-4582544");

    assertEquals(job.id, "da520d24-dd5b-4387-92be-2020a7f2b176");
    assertEquals(job.status, "RUNNING");

    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath(format("/v1.0/123123/domains/%s/records/MX-4582544", domainId));
  }

  CloudDNS mockApi() {
    final TokenIdAndPublicURL tokenIdAndPublicURL = new TokenIdAndPublicURL();
    tokenIdAndPublicURL.tokenId = server.tokenId();
    tokenIdAndPublicURL.publicURL = server.url() + "/v1.0/" + server.tenantId();
    return Feign.create(new CloudDNSTarget(new CloudDNSProvider() {
      @Override
      public String url() {
        return tokenIdAndPublicURL.publicURL;
      }
    }, new javax.inject.Provider<TokenIdAndPublicURL>() {

      @Override
      public TokenIdAndPublicURL get() {
        return tokenIdAndPublicURL;
      }

    }), new CloudDNSProvider.FeignModule());
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
      "{\"domains\":[{\"name\":\"denominator.io\",\"id\":1234,\"accountId\":123123,\"emailAddress\":\"admin@denominator.io\",\"updated\":\"2013-09-02T19:46:56.000+0000\",\"created\":\"2013-09-02T19:45:51.000+0000\"}],\"totalEntries\":1}";
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

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockCloudDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
