package denominator.clouddns;

import static denominator.CredentialsConfiguration.credentials;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.QueueDispatcher;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class CloudDNSZoneApiMockTest {

    String session = "{\"access\":{\"token\":{\"id\":\"b84f4a37-5126-4603-9521-ccd0665fbde1\",\"expires\":\"2013-04-13T16:49:57.000-05:00\",\"tenant\":{\"id\":\"123123\",\"name\":\"123123\"}},\"serviceCatalog\":[{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudMonitoring\",\"type\":\"rax:monitor\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFilesCDN\",\"type\":\"rax:object-cdn\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudLoadBalancers\",\"type\":\"rax:load-balancer\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDatabases\",\"type\":\"rax:database\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFiles\",\"type\":\"object-store\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\",\"versionInfo\":\"URL/v1.0\",\"versionList\":\"URL/\",\"versionId\":\"1.0\"}],\"name\":\"cloudServers\",\"type\":\"compute\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"}],\"name\":\"cloudServersOpenStack\",\"type\":\"compute\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDNS\",\"type\":\"rax:dns\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudBackup\",\"type\":\"rax:backup\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"}],\"name\":\"cloudBlockStorage\",\"type\":\"volume\"}],\"user\":{\"id\":\"1234\",\"roles\":[{\"id\":\"3\",\"description\":\"User Admin Role.\",\"name\":\"identity:user-admin\"}],\"name\":\"jclouds-joe\",\"RAX-AUTH:defaultRegion\":\"DFW\"}}}";
    String zones = "{\"status\": \"success\", \"data\": [\"/REST/Zone/0.0.0.0.d.6.e.0.0.a.2.ip6.arpa/\", \"/REST/Zone/126.12.44.in-addr.arpa/\", \"/REST/Zone/denominator.io/\"], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 3 zones\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("/");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(zones));

        try {
            ZoneApi api = mockApi(url);
            Iterator<Zone> zones = api.iterator();

            while (zones.hasNext()) {
                Zone zone = zones.next();

                assertEquals(zone.name(), "denominator.io");
                assertEquals(zone.id().get(), "1234");
            }

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("/");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(NOT_FOUND.getStatusCode())); // no existing zones

        try {
            ZoneApi api = mockApi(url);

            assertFalse(api.iterator().hasNext());
            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    /**
     * there's no built-in way to defer evaluation of a response header, hence this
     * method, which allows us to send back links to the mock server.
     */
    private QueueDispatcher getURLReplacingQueueDispatcher(final URL url) {
       final QueueDispatcher dispatcher = new QueueDispatcher() {
          protected final BlockingQueue<MockResponse> responseQueue = new LinkedBlockingQueue<MockResponse>();

          @Override
          public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
             MockResponse response = responseQueue.take();
             if (response.getBody() != null) {
                String newBody = new String(response.getBody()).replace("URL", url.toString());
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
    
    private static ZoneApi mockApi(final URL url) {
        return Denominator.create(new CloudDNSProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("jclouds-joe", "letmein")).api().zones();
    }
 }
