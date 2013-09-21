package denominator.clouddns;

import com.google.common.collect.ImmutableList;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.QueueDispatcher;
import com.google.mockwebserver.RecordedRequest;
import denominator.clouddns.RackspaceApis.Record;
import denominator.clouddns.RackspaceApis.JobIdAndStatus;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import denominator.clouddns.RackspaceApis.CloudIdentity;
import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.model.Zone;
import feign.Feign;
import feign.Target.HardCodedTarget;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static org.testng.Assert.*;

@Test(singleThreaded = true)
public class RackspaceApisTest {
    static String username = "joe";
    static String password = "letmein";
    static String tokenId = "b84f4a37-5126-4603-9521-ccd0665fbde1";

    static String session = "{\"access\":{\"token\":{\"id\":\"" + tokenId + "\",\"expires\":\"2013-04-13T16:49:57.000-05:00\",\"tenant\":{\"id\":\"123123\",\"name\":\"123123\"}},\"serviceCatalog\":[{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudMonitoring\",\"type\":\"rax:monitor\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFilesCDN\",\"type\":\"rax:object-cdn\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudLoadBalancers\",\"type\":\"rax:load-balancer\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDatabases\",\"type\":\"rax:database\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFiles\",\"type\":\"object-store\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\",\"versionInfo\":\"URL/v1.0\",\"versionList\":\"URL/\",\"versionId\":\"1.0\"}],\"name\":\"cloudServers\",\"type\":\"compute\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"}],\"name\":\"cloudServersOpenStack\",\"type\":\"compute\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDNS\",\"type\":\"rax:dns\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudBackup\",\"type\":\"rax:backup\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"}],\"name\":\"cloudBlockStorage\",\"type\":\"volume\"}],\"user\":{\"id\":\"1234\",\"roles\":[{\"id\":\"3\",\"description\":\"User Admin Role.\",\"name\":\"identity:user-admin\"}],\"name\":\"jclouds-joe\",\"RAX-AUTH:defaultRegion\":\"DFW\"}}}";

    static String auth = format(
            "{\"auth\":{\"passwordCredentials\":{\"username\":\"%s\",\"password\":\"%s\"}}}", username, password);

    @Test
    public void authSuccess() throws IOException, InterruptedException, URISyntaxException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.play();

        try {
            CloudIdentity api = Feign.create(new HardCodedTarget<CloudIdentity>(CloudIdentity.class, "not used!!"),
                    new CloudDNSProvider.FeignModule());
            TokenIdAndPublicURL tokenIdAndPublicURL = api.passwordAuth(
                    URI.create("http://localhost:" + server.getPort()), username, password);

            assertEquals(tokenIdAndPublicURL.tokenId, tokenId);
            assertEquals(tokenIdAndPublicURL.publicURL, "URL/v1.0/123123");

            takeAuthResponse(server);
        } finally {
            server.shutdown();
        }
    }

    static String limitsResponse = "{\"limits\":{\"rate\":[{\"regex\":\".*/v\\d+\\.\\d+/(\\d+/status).*\",\"uri\":\"*/status/*\",\"limit\":[{\"next-available\":\"2013-09-02T20:30:12.063Z\",\"unit\":\"SECOND\",\"remaining\":5,\"value\":5,\"verb\":\"GET\"}]},{\"regex\":\".*/v\\d+\\.\\d+/(\\d+/domains).*\",\"uri\":\"*/domains*\",\"limit\":[{\"next-available\":\"2013-09-02T20:30:12.063Z\",\"unit\":\"MINUTE\",\"remaining\":100,\"value\":100,\"verb\":\"GET\"},{\"next-available\":\"2013-09-02T20:30:12.063Z\",\"unit\":\"MINUTE\",\"remaining\":25,\"value\":25,\"verb\":\"POST\"},{\"next-available\":\"2013-09-02T20:30:12.064Z\",\"unit\":\"MINUTE\",\"remaining\":50,\"value\":50,\"verb\":\"PUT\"},{\"next-available\":\"2013-09-02T20:30:12.064Z\",\"unit\":\"MINUTE\",\"remaining\":50,\"value\":50,\"verb\":\"DELETE\"}]}],\"absolute\":{\"domains\":500,\"recordsperdomain\":500}}}";

    @Test
    public void limitsSuccess() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(limitsResponse));
        server.play();

        try {
            assertFalse(mockApi(server.getPort()).limits().isEmpty());

            assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/limits HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    static int domainId = 3854937;
    static String domainsResponse = "{\"domains\":[{\"name\":\"denominator.io\",\"id\":3854937,\"accountId\":123123,\"emailAddress\":\"admin@denominator.io\",\"updated\":\"2013-09-02T19:46:56.000+0000\",\"created\":\"2013-09-02T19:45:51.000+0000\"}],\"totalEntries\":1}";

    @Test
    public void domainsPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(domainsResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).domains(),
                    ImmutableList.of(Zone.create("denominator.io", String.valueOf(domainId))));

            assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void domainsAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"domains\":[]}"));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).domains().isEmpty());

            assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    // NOTE records are allowed to be out of order by type
    static String recordsResponse = "{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"A-10465369\",\"type\":\"A\",\"data\":\"192.0.2.1\",\"ttl\":300,\"updated\":\"2013-09-02T20:51:55.000+0000\",\"created\":\"2013-09-02T20:51:55.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"A-10465370\",\"type\":\"A\",\"data\":\"192.0.2.2\",\"ttl\":300,\"updated\":\"2013-09-02T20:52:07.000+0000\",\"created\":\"2013-09-02T20:52:07.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-9084762\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":300,\"updated\":\"2013-09-02T20:51:12.000+0000\",\"created\":\"2013-09-02T20:51:12.000+0000\"},{\"name\":\"www.denominator.io\",\"id\":\"NS-9084763\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":300,\"updated\":\"2013-09-02T20:51:12.000+0000\",\"created\":\"2013-09-02T20:51:12.000+0000\"}],\"totalEntries\":4}";

    @Test
    public void recordsPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(recordsResponse));
        server.play();

        try {
            List<Record> records = mockApi(server.getPort()).records(domainId);
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

            assertEquals(server.takeRequest().getRequestLine(), format("GET /v1.0/123123/domains/%s/records HTTP/1.1", domainId));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void recordsAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"records\":[]}"));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).records(domainId).isEmpty());

            assertEquals(server.takeRequest().getRequestLine(), format("GET /v1.0/123123/domains/%s/records HTTP/1.1", domainId));

        } finally {
            server.shutdown();
        }
    }

    static String mxRecordCreateRequest = "{\"records\":[{\"name\":\"www.denominator.io\",\"type\":\"MX\",\"ttl\":\"1800\",\"data\":\"mail.denominator.io\",\"priority\":\"10\"}]}";
    static String mxRecordInitialResponse = "{\"request\":\"{\\\"records\\\":[{\\\"name\\\":\\\"www.denominator.io\\\",\\\"type\\\":\\\"MX\\\",\\\"ttl\\\":\\\"1800\\\",\\\"data\\\":\\\"mail.denominator.io\\\",\\\"priority\\\":\\\"10\\\"}]}\",\"status\":\"RUNNING\",\"verb\":\"POST\",\"jobId\":\"0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records\"}";
    static String mxRecordRunningResponse = "{\"request\":\"{\\\"records\\\":[{\\\"name\\\":\\\"www.denominator.io\\\",\\\"type\\\":\\\"MX\\\",\\\"ttl\\\":\\\"1800\\\",\\\"data\\\":\\\"mail.denominator.io\\\",\\\"priority\\\":\\\"10\\\"}]}\",\"status\":\"RUNNING\",\"verb\":\"POST\",\"jobId\":\"0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records\"}";
    static String mxRecordCompletedResponse = "{\"request\":\"{\\\"records\\\":[{\\\"name\\\":\\\"www.denominator.io\\\",\\\"type\\\":\\\"MX\\\",\\\"ttl\\\":\\\"1800\\\",\\\"data\\\":\\\"mail.denominator.io\\\",\\\"priority\\\":\\\"10\\\"}]}\",\"response\":{\"records\":[{\"name\":\"www.denominator.io\",\"id\":\"MX-4582544\",\"priority\":10,\"type\":\"MX\",\"data\":\"mail.denominator.io\",\"ttl\":1800,\"updated\":\"2013-09-02T21:10:03.000+0000\",\"created\":\"2013-09-02T21:10:03.000+0000\"}]},\"status\":\"COMPLETED\",\"verb\":\"POST\",\"jobId\":\"0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records\"}";

    @Test
    public void createMXRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(mxRecordInitialResponse));
        server.enqueue(new MockResponse().setBody(mxRecordRunningResponse));
        server.enqueue(new MockResponse().setBody(mxRecordCompletedResponse));
        server.play();

        try {
            JobIdAndStatus job = mockApi(server.getPort()).
                    createRecordWithPriority(domainId, "www.denominator.io", "MX", 1800, "mail.denominator.io", 10);

            assertEquals(job.id, "0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
            assertEquals(job.status, "RUNNING");

            job = mockApi(server.getPort()).getStatus("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");

            assertEquals(job.id, "0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
            assertEquals(job.status, "RUNNING");

            job = mockApi(server.getPort()).getStatus("0ade2b3b-07e4-4e68-821a-fcce4f5406f3");

            assertEquals(job.id, "0ade2b3b-07e4-4e68-821a-fcce4f5406f3");
            assertEquals(job.status, "COMPLETED");

            RecordedRequest createRequest = server.takeRequest();
            assertEquals(createRequest.getRequestLine(), format("POST /v1.0/123123/domains/%s/records HTTP/1.1", domainId));
            assertEquals(new String(createRequest.getBody()), mxRecordCreateRequest);

            RecordedRequest runningRequest = server.takeRequest();
            assertEquals(runningRequest.getRequestLine(), "GET /v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3?showDetails=true HTTP/1.1");

            RecordedRequest completedRequest = server.takeRequest();
            assertEquals(completedRequest.getRequestLine(), "GET /v1.0/123123/status/0ade2b3b-07e4-4e68-821a-fcce4f5406f3?showDetails=true HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    static String mxRecordUpdateRequest = "{\"ttl\":\"600\",\"data\":\"mail.denominator.io\"}";
    static String mxRecordUpdateInitialResponse = "{\"request\":\"{\\\"ttl\\\":\\\"600\\\",\\\"data\\\":\\\"mail.denominator.io\\\"}\",\"status\":\"RUNNING\",\"verb\":\"PUT\",\"jobId\":\"e32eace1-c44f-49af-8f74-768fa34469f4\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/e32eace1-c44f-49af-8f74-768fa34469f4\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";
    static String mxRecordUpdateRunningResponse = "{\"request\":\"{\\\"ttl\\\":\\\"600\\\",\\\"data\\\":\\\"mail.denominator.io\\\"}\",\"status\":\"RUNNING\",\"verb\":\"PUT\",\"jobId\":\"e32eace1-c44f-49af-8f74-768fa34469f4\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/e32eace1-c44f-49af-8f74-768fa34469f4\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";
    static String mxRecordUpdateCompletedResponse = "{\"request\":\"{\\\"ttl\\\":\\\"600\\\",\\\"data\\\":\\\"mail.denominator.io\\\"}\",\"status\":\"COMPLETED\",\"verb\":\"PUT\",\"jobId\":\"e32eace1-c44f-49af-8f74-768fa34469f4\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/e32eace1-c44f-49af-8f74-768fa34469f4\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";

    @Test
    public void updateMXRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(mxRecordUpdateInitialResponse));
        server.enqueue(new MockResponse().setBody(mxRecordUpdateRunningResponse));
        server.enqueue(new MockResponse().setBody(mxRecordUpdateCompletedResponse));
        server.play();

        try {
            JobIdAndStatus job = mockApi(server.getPort()).
                    updateRecord(domainId, "MX-4582544", 600, "mail.denominator.io");

            assertEquals(job.id, "e32eace1-c44f-49af-8f74-768fa34469f4");
            assertEquals(job.status, "RUNNING");

            job = mockApi(server.getPort()).getStatus("e32eace1-c44f-49af-8f74-768fa34469f4");

            assertEquals(job.id, "e32eace1-c44f-49af-8f74-768fa34469f4");
            assertEquals(job.status, "RUNNING");

            job = mockApi(server.getPort()).getStatus("e32eace1-c44f-49af-8f74-768fa34469f4");

            assertEquals(job.id, "e32eace1-c44f-49af-8f74-768fa34469f4");
            assertEquals(job.status, "COMPLETED");

            RecordedRequest updateRequest = server.takeRequest();
            assertEquals(updateRequest.getRequestLine(), format("PUT /v1.0/123123/domains/%s/records/MX-4582544 HTTP/1.1", domainId));
            assertEquals(new String(updateRequest.getBody()), mxRecordUpdateRequest);

            RecordedRequest runningRequest = server.takeRequest();
            assertEquals(runningRequest.getRequestLine(), "GET /v1.0/123123/status/e32eace1-c44f-49af-8f74-768fa34469f4?showDetails=true HTTP/1.1");

            RecordedRequest completedRequest = server.takeRequest();
            assertEquals(completedRequest.getRequestLine(), "GET /v1.0/123123/status/e32eace1-c44f-49af-8f74-768fa34469f4?showDetails=true HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    static String mxRecordDeleteInitialResponse = "{\"status\":\"RUNNING\",\"verb\":\"DELETE\",\"jobId\":\"da520d24-dd5b-4387-92be-2020a7f2b176\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/da520d24-dd5b-4387-92be-2020a7f2b176\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";
    static String mxRecordDeleteRunningResponse = "{\"status\":\"RUNNING\",\"verb\":\"DELETE\",\"jobId\":\"da520d24-dd5b-4387-92be-2020a7f2b176\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/da520d24-dd5b-4387-92be-2020a7f2b176\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";
    static String mxRecordDeleteCompletedResponse = "{\"status\":\"COMPLETED\",\"verb\":\"DELETE\",\"jobId\":\"da520d24-dd5b-4387-92be-2020a7f2b176\",\"callbackUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/status/da520d24-dd5b-4387-92be-2020a7f2b176\",\"requestUrl\":\"https://dns.api.rackspacecloud.com/v1.0/123123/domains/3854989/records/MX-4582544\"}";

    @Test
    public void deleteRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(mxRecordDeleteInitialResponse));
        server.enqueue(new MockResponse().setBody(mxRecordDeleteRunningResponse));
        server.enqueue(new MockResponse().setBody(mxRecordDeleteCompletedResponse));
        server.play();

        try {
            JobIdAndStatus job = mockApi(server.getPort()).deleteRecord(domainId, "MX-4582544");

            assertEquals(job.id, "da520d24-dd5b-4387-92be-2020a7f2b176");
            assertEquals(job.status, "RUNNING");

            job = mockApi(server.getPort()).getStatus("da520d24-dd5b-4387-92be-2020a7f2b176");

            assertEquals(job.id, "da520d24-dd5b-4387-92be-2020a7f2b176");
            assertEquals(job.status, "RUNNING");

            job = mockApi(server.getPort()).getStatus("da520d24-dd5b-4387-92be-2020a7f2b176");

            assertEquals(job.id, "da520d24-dd5b-4387-92be-2020a7f2b176");
            assertEquals(job.status, "COMPLETED");

            RecordedRequest deleteRequest = server.takeRequest();
            assertEquals(deleteRequest.getRequestLine(), format("DELETE /v1.0/123123/domains/%s/records/MX-4582544 HTTP/1.1", domainId));

            RecordedRequest runningRequest = server.takeRequest();
            assertEquals(runningRequest.getRequestLine(), "GET /v1.0/123123/status/da520d24-dd5b-4387-92be-2020a7f2b176?showDetails=true HTTP/1.1");

            RecordedRequest completedRequest = server.takeRequest();
            assertEquals(completedRequest.getRequestLine(), "GET /v1.0/123123/status/da520d24-dd5b-4387-92be-2020a7f2b176?showDetails=true HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    static CloudDNS mockApi(final int port) {
        final TokenIdAndPublicURL tokenIdAndPublicURL = new TokenIdAndPublicURL();
        tokenIdAndPublicURL.tokenId = tokenId;
        tokenIdAndPublicURL.publicURL = "http://localhost:" + port + "/v1.0/123123";
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

    static void takeAuthResponse(MockWebServer server) throws InterruptedException {
        RecordedRequest authRequest = server.takeRequest();
        assertEquals(authRequest.getRequestLine(), "POST /tokens HTTP/1.1");
        assertEquals(new String(authRequest.getBody()), auth);
    }

    /**
     * there's no built-in way to defer evaluation of a response header, hence
     * this method, which allows us to send back links to the mock server.
     */
    static QueueDispatcher getURLReplacingQueueDispatcher(final String url) {
        return getURLReplacingQueueDispatcher(new AtomicReference<String>(url));
    }

    static QueueDispatcher getURLReplacingQueueDispatcher(final AtomicReference<String> url) {
        final QueueDispatcher dispatcher = new QueueDispatcher() {
            protected final BlockingQueue<MockResponse> responseQueue = new LinkedBlockingQueue<MockResponse>();

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                MockResponse response = responseQueue.take();
                if (response.getBody() != null) {
                    String newBody = new String(response.getBody()).replace(": \"URL", ": \"" + url);
                    response = response.setBody(newBody);
                }
                return response;
            }

            @Override
            public void enqueueResponse(MockResponse response) {
                responseQueue.add(response);
            }
        };

        return dispatcher;
    }
}
