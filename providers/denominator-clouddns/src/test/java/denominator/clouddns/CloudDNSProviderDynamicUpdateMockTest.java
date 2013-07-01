package denominator.clouddns;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.common.base.Supplier;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.QueueDispatcher;
import com.google.mockwebserver.RecordedRequest;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

@Test(singleThreaded = true)
public class CloudDNSProviderDynamicUpdateMockTest {

    String session = "{\"access\":{\"token\":{\"id\":\"b84f4a37-5126-4603-9521-ccd0665fbde1\",\"expires\":\"2013-04-13T16:49:57.000-05:00\",\"tenant\":{\"id\":\"123123\",\"name\":\"123123\"}},\"serviceCatalog\":[{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudMonitoring\",\"type\":\"rax:monitor\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFilesCDN\",\"type\":\"rax:object-cdn\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudLoadBalancers\",\"type\":\"rax:load-balancer\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDatabases\",\"type\":\"rax:database\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFiles\",\"type\":\"object-store\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\",\"versionInfo\":\"URL/v1.0\",\"versionList\":\"URL/\",\"versionId\":\"1.0\"}],\"name\":\"cloudServers\",\"type\":\"compute\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"}],\"name\":\"cloudServersOpenStack\",\"type\":\"compute\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDNS\",\"type\":\"rax:dns\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudBackup\",\"type\":\"rax:backup\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"}],\"name\":\"cloudBlockStorage\",\"type\":\"volume\"}],\"user\":{\"id\":\"1234\",\"roles\":[{\"id\":\"3\",\"description\":\"User Admin Role.\",\"name\":\"identity:user-admin\"}],\"name\":\"jclouds-joe\",\"RAX-AUTH:defaultRegion\":\"DFW\"}}}";

    @Test
    public void dynamicEndpointUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        String initialPath = "";
        String updatedPath = "alt";
        URL mockUrl = server.getUrl(initialPath);
        final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);
        server.setDispatcher(getURLReplacingQueueDispatcher(dynamicUrl));

        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(
                "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(
                "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));
        try {
            DNSApi api = Denominator.create(new CloudDNSProvider() {
                @Override
                public String url() {
                    return dynamicUrl.get().toString();
                }
            }, credentials("jclouds-joe", "letmein")).api();

            assertFalse(api.zones().iterator().hasNext());
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertFalse(api.zones().iterator().hasNext());

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/domains HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "POST /alt/tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /alt/v1.0/123123/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicCredentialUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        final URL mockUrl = server.getUrl("");
        server.setDispatcher(getURLReplacingQueueDispatcher(new AtomicReference<URL>(mockUrl)));

        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(
                "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(
                "{\"message\":\"Not Found\",\"code\":404,\"details\":\"\"}"));
        try {

            final AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(
                    ListCredentials.from("jclouds-joe", "letmein"));

            DNSApi api = Denominator.create(new CloudDNSProvider() {
                @Override
                public String url() {
                    return mockUrl.toString();
                }
            }, credentials(new Supplier<Credentials>() {
                @Override
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            })).api();

            assertFalse(api.zones().iterator().hasNext());
            dynamicCredentials.set(ListCredentials.from("jclouds-bob", "comeon"));
            assertFalse(api.zones().iterator().hasNext());

            assertEquals(server.getRequestCount(), 4);
            assertEquals(new String(server.takeRequest().getBody()),
                    "{\"auth\":{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"jclouds-joe\",\"apiKey\":\"letmein\"}}}");
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/domains HTTP/1.1");
            assertEquals(new String(server.takeRequest().getBody()),
                    "{\"auth\":{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"jclouds-bob\",\"apiKey\":\"comeon\"}}}");
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    /**
     * there's no built-in way to defer evaluation of a response header, hence
     * this method, which allows us to send back links to the mock server.
     */
    private QueueDispatcher getURLReplacingQueueDispatcher(final AtomicReference<URL> dynamicUrl) {
        return new QueueDispatcher() {
            protected final BlockingQueue<MockResponse> responseQueue = new LinkedBlockingQueue<MockResponse>();

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                MockResponse response = responseQueue.take();
                if (response.getBody() != null) {
                    String newBody = new String(response.getBody()).replace(":\"URL", ":\""
                            + dynamicUrl.get().toString());
                    response = response.setBody(newBody);
                }
                return response;
            }

            @Override
            public void enqueueResponse(MockResponse response) {
                responseQueue.add(response);
            }
        };
    }
}
