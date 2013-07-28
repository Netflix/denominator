package denominator.designate;

import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.QueueDispatcher;
import com.google.mockwebserver.RecordedRequest;

import denominator.designate.Designate.Record;
import denominator.designate.KeystoneV2.TokenIdAndPublicURL;
import denominator.model.Zone;
import feign.Feign;
import feign.Target.HardCodedTarget;

@Test(singleThreaded = true)
public class DesignateTest {
    static String tenantId = "123123";
    static String username = "joe";
    static String password = "letmein";

    static String auth = format(
            "{\"auth\":{\"passwordCredentials\":{\"username\":\"%s\",\"password\":\"%s\"},\"tenantId\":\"%s\"}}",
            username, password, tenantId);

    static String tokenId = "b84f4a37-5126-4603-9521-ccd0665fbde1";

    static String accessResponse = "" //
            + "{\"access\": {\n" //
            + "  \"token\": {\n" //
            + "    \"expires\": \"2013-07-08T05:55:31.809Z\",\n" //
            + format("    \"id\": \"%s\",\n", tokenId) //
            + "    \"tenant\": {\n" //
            + format("      \"id\": \"%s\",\n", tenantId) //
            + "      \"name\": \"denominator\"\n" //
            + "    }\n" //
            + "  },\n" //
            + "  \"serviceCatalog\": [\n" //
            + "    {\n" //
            + "      \"name\": \"Identity\",\n" //
            + "      \"type\": \"identity\",\n" //
            + "      \"endpoints\": [\n" //
            + "        {\n" //
            + "          \"publicURL\": \"URL\\/v2.0\\/\",\n" //
            + "          \"region\": \"region-a.geo-1\",\n" //
            + "          \"versionId\": \"2.0\",\n" //
            + "          \"versionInfo\": \"URL\\/v2.0\\/\",\n" //
            + "          \"versionList\": \"URL\"\n" //
            + "        },\n" //
            + "        {\n" //
            + "          \"publicURL\": \"URL\\/v3\\/\",\n" //
            + "          \"region\": \"region-a.geo-1\",\n" //
            + "          \"versionId\": \"3.0\",\n" //
            + "          \"versionInfo\": \"URL\\/v3\\/\",\n" //
            + "          \"versionList\": \"URL\"\n" //
            + "        }\n" //
            + "      ]\n" //
            + "    },\n" //
            + "    {\n" //
            + "      \"name\": \"DNS\",\n" //
            + "      \"type\": \"hpext:dns\",\n" //
            + "      \"endpoints\": [{\n" //
            + "        \"tenantId\": \"10448598368512\",\n" //
            + "        \"publicURL\": \"URL\\/v1\\/\",\n" //
            + "        \"publicURL2\": \"\",\n" //
            + "        \"region\": \"region-a.geo-1\",\n" //
            + "        \"versionId\": \"1\",\n" //
            + "        \"versionInfo\": \"URL\\/v1\\/\",\n" //
            + "        \"versionList\": \"URL\\/\"\n" //
            + "      }]\n" //
            + "    }\n" //
            + "  ]\n" //
            + "}}";

    @Test
    public void authSuccess() throws IOException, InterruptedException, URISyntaxException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(accessResponse));
        server.play();

        try {
            KeystoneV2 api = Feign.create(new HardCodedTarget<KeystoneV2>(KeystoneV2.class, "not used!!"),
                    new DesignateProvider.FeignModule());
            TokenIdAndPublicURL tokenIdAndPublicURL = api.passwordAuth(server.getUrl("").toURI(), tenantId, username,
                    password);

            assertEquals(tokenIdAndPublicURL.tokenId, tokenId);
            assertEquals(tokenIdAndPublicURL.publicURL, "URL/v1");

            takeAuthResponse(server);
        } finally {
            server.shutdown();
        }
    }

    static String limitsResponse = ""//
            + "{\n" //
            + "  \"limits\": {\n" //
            + "    \"absolute\": {\n" //
            + "      \"maxDomains\": 20,\n" //
            + "      \"maxDomainRecords\": 5000\n" //
            + "    }\n" //
            + "  }\n" //
            + "}";

    @Test
    public void limitsSuccess() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(limitsResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getUrl("")).limits(), limitsResponse);

            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/limits HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    static String domainId = "62ac4caf-6108-4b74-b6fe-460967db32a7";

    static String domainsResponse = ""//
            + "{\n" //
            + "  \"domains\": [\n" //
            + "    {\n" //
            + "      \"name\": \"denominator.io.\",\n" //
            + "      \"created_at\": \"2013-07-07T17:55:21.000000\",\n" //
            + "      \"updated_at\": null,\n" //
            + "      \"email\": \"admin@denominator.io\",\n" //
            + "      \"ttl\": 3600,\n" //
            + "      \"serial\": 1373219721,\n" //
            + format("      \"id\": \"%s\"\n", domainId) //
            + "    }\n" //
            + "  ]\n" //
            + "}";

    @Test
    public void domainsPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(domainsResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getUrl("")).domains(),
                    ImmutableList.of(Zone.create("denominator.io.", domainId)));

            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void domainsAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));
        server.play();

        try {
            assertTrue(mockApi(server.getUrl("")).domains().isEmpty());

            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    // NOTE records are allowed to be out of order by type
    static String recordsResponse = ""//
            + "{\n" //
            + "  \"records\": [\n" //
            + "    {\n" //
            + "      \"name\": \"www.denominator.io.\",\n" //
            + "      \"data\": \"192.0.2.2\",\n" //
            + "      \"created_at\": \"2013-07-07T18:09:47.000000\",\n" //
            + "      \"updated_at\": null,\n" //
            + "      \"id\": \"c538d70e-d65f-4d5a-92a2-cd5d4d1d9da4\",\n" //
            + "      \"priority\": null,\n" //
            + "      \"ttl\": 300,\n" //
            + "      \"type\": \"A\",\n" //
            + format("      \"domain_id\": \"%s\"\n", domainId) //
            + "    },\n" //
            + "    {\n" //
            + "      \"name\": \"denominator.io.\",\n" //
            + "      \"data\": \"www.denominator.io.\",\n" //
            + "      \"created_at\": \"2013-07-07T18:09:47.000000\",\n" //
            + "      \"updated_at\": null,\n" //
            + "      \"id\": \"13d2516b-1f18-455b-aa05-1997b26192ad\",\n" //
            + "      \"priority\": 10,\n" //
            + "      \"ttl\": 300,\n" //
            + "      \"type\": \"MX\",\n" //
            + format("      \"domain_id\": \"%s\"\n", domainId) //
            + "    },\n" //
            + "    {\n" //
            + "      \"name\": \"www.denominator.io.\",\n" //
            + "      \"data\": \"192.0.2.1\",\n" //
            + "      \"created_at\": \"2013-07-07T18:09:31.000000\",\n" //
            + "      \"updated_at\": null,\n" //
            + "      \"id\": \"d7eb0fc4-e069-4c92-a272-c5c969b4f558\",\n" //
            + "      \"priority\": null,\n" //
            + "      \"ttl\": 300,\n" //
            + "      \"type\": \"A\",\n" //
            + format("      \"domain_id\": \"%s\"\n", domainId) //
            + "    }\n" //
            + "  ]\n" //
            + "}";

    @Test
    public void recordsPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(recordsResponse));
        server.play();

        try {
            List<Record> records = mockApi(server.getUrl("")).records(domainId);
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

            assertEquals(server.takeRequest().getRequestLine(), format("GET /v1/domains/%s/records HTTP/1.1", domainId));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void recordsAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{ \"records\": [] }"));
        server.play();

        try {
            assertTrue(mockApi(server.getUrl("")).records(domainId).isEmpty());

            assertEquals(server.takeRequest().getRequestLine(), format("GET /v1/domains/%s/records HTTP/1.1", domainId));

        } finally {
            server.shutdown();
        }
    }

    static String mxRecordResponse = ""//
            + "{\n" //
            + "  \"name\": \"denominator.io.\",\n" //
            + "  \"data\": \"www.denominator.io.\",\n" //
            + "  \"created_at\": \"2013-07-07T18:09:47.000000\",\n" //
            + "  \"updated_at\": null,\n" //
            + "  \"id\": \"13d2516b-1f18-455b-aa05-1997b26192ad\",\n" //
            + "  \"priority\": 10,\n" //
            + "  \"ttl\": 300,\n" //
            + "  \"type\": \"MX\",\n" //
            + format("  \"domain_id\": \"%s\"\n", domainId) //
            + "}";

    @Test
    public void createMXRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(mxRecordResponse));
        server.play();

        try {
            Record record = new Record();
            record.name = "denominator.io.";
            record.data = "www.denominator.io.";
            record.priority = 10;
            record.ttl = 300;
            record.type = "MX";

            record = mockApi(server.getUrl("")).createRecord(domainId, record);

            assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
            assertEquals(record.name, "denominator.io.");
            assertEquals(record.data, "www.denominator.io.");
            assertEquals(record.priority.intValue(), 10);
            assertEquals(record.ttl.intValue(), 300);
            assertEquals(record.type, "MX");

            RecordedRequest createRequest = server.takeRequest();
            assertEquals(createRequest.getRequestLine(), format("POST /v1/domains/%s/records HTTP/1.1", domainId));
            assertEquals(new String(createRequest.getBody()),
                    "{\"name\":\"denominator.io.\",\"type\":\"MX\",\"ttl\":300,\"data\":\"www.denominator.io.\",\"priority\":10}");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void updateMXRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(mxRecordResponse));
        server.play();

        try {
            Record record = new Record();
            record.id = "13d2516b-1f18-455b-aa05-1997b26192ad";
            record.name = "denominator.io.";
            record.data = "www.denominator.io.";
            record.priority = 10;
            record.ttl = 300;
            record.type = "MX";

            record = mockApi(server.getUrl("")).updateRecord(domainId, record.id, record);

            assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
            assertEquals(record.name, "denominator.io.");
            assertEquals(record.data, "www.denominator.io.");
            assertEquals(record.priority.intValue(), 10);
            assertEquals(record.ttl.intValue(), 300);
            assertEquals(record.type, "MX");

            RecordedRequest updateRequest = server.takeRequest();
            assertEquals(updateRequest.getRequestLine(),
                    format("PUT /v1/domains/%s/records/%s HTTP/1.1", domainId, record.id));
            assertEquals(new String(updateRequest.getBody()),
                    "{\"name\":\"denominator.io.\",\"type\":\"MX\",\"ttl\":300,\"data\":\"www.denominator.io.\",\"priority\":10}");

        } finally {
            server.shutdown();
        }
    }

    static String aRecordResponse = ""//
            + "{\n" //
            + "  \"name\": \"www.denominator.io.\",\n" //
            + "  \"data\": \"192.0.2.1\",\n" //
            + "  \"created_at\": \"2013-07-07T18:09:47.000000\",\n" //
            + "  \"updated_at\": null,\n" //
            + "  \"id\": \"13d2516b-1f18-455b-aa05-1997b26192ad\",\n" //
            + "  \"ttl\": null,\n" //
            + "  \"type\": \"A\",\n" //
            + format("  \"domain_id\": \"%s\"\n", domainId) //
            + "}";

    @Test
    public void createARecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(aRecordResponse));
        server.play();

        try {
            Record record = new Record();
            record.name = "www.denominator.io.";
            record.data = "192.0.2.1";
            record.type = "A";

            record = mockApi(server.getUrl("")).createRecord(domainId, record);

            assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
            assertEquals(record.name, "www.denominator.io.");
            assertEquals(record.data, "192.0.2.1");
            assertNull(record.priority);
            assertNull(record.ttl);
            assertEquals(record.type, "A");

            RecordedRequest createRequest = server.takeRequest();
            assertEquals(createRequest.getRequestLine(), format("POST /v1/domains/%s/records HTTP/1.1", domainId));
            assertEquals(new String(createRequest.getBody()),
                    "{\"name\":\"www.denominator.io.\",\"type\":\"A\",\"data\":\"192.0.2.1\"}");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void updateARecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(aRecordResponse));
        server.play();

        try {
            Record record = new Record();
            record.id = "13d2516b-1f18-455b-aa05-1997b26192ad";
            record.name = "www.denominator.io.";
            record.data = "192.0.2.1";
            record.type = "A";

            record = mockApi(server.getUrl("")).updateRecord(domainId, record.id, record);

            assertEquals(record.id, "13d2516b-1f18-455b-aa05-1997b26192ad");
            assertEquals(record.name, "www.denominator.io.");
            assertEquals(record.data, "192.0.2.1");
            assertNull(record.priority);
            assertNull(record.ttl);
            assertEquals(record.type, "A");

            RecordedRequest updateRequest = server.takeRequest();
            assertEquals(updateRequest.getRequestLine(),
                    format("PUT /v1/domains/%s/records/%s HTTP/1.1", domainId, record.id));
            assertEquals(new String(updateRequest.getBody()),
                    "{\"name\":\"www.denominator.io.\",\"type\":\"A\",\"data\":\"192.0.2.1\"}");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deleteRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse());
        server.play();

        try {
            String recordId = "13d2516b-1f18-455b-aa05-1997b26192ad";

            mockApi(server.getUrl("")).deleteRecord(domainId, recordId);

            assertEquals(server.takeRequest().getRequestLine(),
                    format("DELETE /v1/domains/%s/records/%s HTTP/1.1", domainId, recordId));
        } finally {
            server.shutdown();
        }
    }

    static Designate mockApi(final URL url) {
        final TokenIdAndPublicURL tokenIdAndPublicURL = new TokenIdAndPublicURL();
        tokenIdAndPublicURL.tokenId = tokenId;
        tokenIdAndPublicURL.publicURL = url.toString() + "/v1";
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

    static void takeAuthResponse(MockWebServer server) throws InterruptedException {
        RecordedRequest authRequest = server.takeRequest();
        assertEquals(authRequest.getRequestLine(), "POST /tokens HTTP/1.1");
        assertEquals(new String(authRequest.getBody()), auth);
    }

    /**
     * there's no built-in way to defer evaluation of a response header, hence
     * this method, which allows us to send back links to the mock server.
     */
    static QueueDispatcher getURLReplacingQueueDispatcher(final URL url) {
        return getURLReplacingQueueDispatcher(new AtomicReference<URL>(url));
    }

    static QueueDispatcher getURLReplacingQueueDispatcher(final AtomicReference<URL> url) {
        final QueueDispatcher dispatcher = new QueueDispatcher() {
            protected final BlockingQueue<MockResponse> responseQueue = new LinkedBlockingQueue<MockResponse>();

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                MockResponse response = responseQueue.take();
                if (response.getBody() != null) {
                    String newBody = new String(response.getBody()).replace(": \"URL", ": \"" + url.toString());
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
