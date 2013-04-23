package denominator.clouddns;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static denominator.model.ResourceRecordSets.a;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.rackspace.clouddns.us.CloudDNSUSProviderMetadata;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.features.RecordApi;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.QueueDispatcher;
import com.google.mockwebserver.RecordedRequest;

import denominator.model.ResourceRecordSet;

@Test(singleThreaded = true)
public class CloudDNSResourceRecordSetApiMockTest {
    static Set<Module> modules = ImmutableSet.<Module> of(new ExecutorServiceModule(sameThreadExecutor(),
            sameThreadExecutor()));

    static RecordApi mockCloudDNSApi(String uri, int domainId) {
        Properties overrides = new Properties();
        overrides.setProperty(PROPERTY_MAX_RETRIES, "1");
        
        CloudDNSApi cloudDNSApi = ContextBuilder.newBuilder(new CloudDNSUSProviderMetadata())
                .credentials("jclouds-joe", "letmein")
                .modules(modules)
                .endpoint(uri)
                .overrides(overrides)
        		.buildApi(CloudDNSApi.class);
        
        return cloudDNSApi.getRecordApiForDomain(domainId);
    }

    String session = "{\"access\":{\"token\":{\"id\":\"b84f4a37-5126-4603-9521-ccd0665fbde1\",\"expires\":\"2013-04-13T16:49:57.000-05:00\",\"tenant\":{\"id\":\"123123\",\"name\":\"123123\"}},\"serviceCatalog\":[{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudMonitoring\",\"type\":\"rax:monitor\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFilesCDN\",\"type\":\"rax:object-cdn\"},{\"endpoints\":[{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudLoadBalancers\",\"type\":\"rax:load-balancer\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDatabases\",\"type\":\"rax:database\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"},{\"region\":\"ORD\",\"tenantId\":\"MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"publicURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\",\"internalURL\":\"URL/v1/MossoCloudFS_5bcf396e-39dd-45ff-93a1-712b9aba90a9\"}],\"name\":\"cloudFiles\",\"type\":\"object-store\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\",\"versionInfo\":\"URL/v1.0\",\"versionList\":\"URL/\",\"versionId\":\"1.0\"}],\"name\":\"cloudServers\",\"type\":\"compute\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v2/123123\",\"versionInfo\":\"URL/v2\",\"versionList\":\"URL/\",\"versionId\":\"2\"}],\"name\":\"cloudServersOpenStack\",\"type\":\"compute\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudDNS\",\"type\":\"rax:dns\"},{\"endpoints\":[{\"tenantId\":\"123123\",\"publicURL\":\"URL/v1.0/123123\"}],\"name\":\"cloudBackup\",\"type\":\"rax:backup\"},{\"endpoints\":[{\"region\":\"DFW\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"},{\"region\":\"ORD\",\"tenantId\":\"123123\",\"publicURL\":\"URL/v1/123123\"}],\"name\":\"cloudBlockStorage\",\"type\":\"volume\"}],\"user\":{\"id\":\"1234\",\"roles\":[{\"id\":\"3\",\"description\":\"User Admin Role.\",\"name\":\"identity:user-admin\"}],\"name\":\"jclouds-joe\",\"RAX-AUTH:defaultRegion\":\"DFW\"}}}";
    String recordsByName = "{\"records\":[{\"name\":\"www.foo.com\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.foo.com\",\"id\":\"NS-8703385\",\"type\":\"NS\",\"data\":\"dns1.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.foo.com\",\"id\":\"NS-8703386\",\"type\":\"NS\",\"data\":\"dns2.stabletransit.com\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"}],\"totalEntries\":3}";

    @Test
    public void listByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("/");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(recordsByName));

        try {
            CloudDNSResourceRecordSetApi api = new CloudDNSResourceRecordSetApi(mockCloudDNSApi(url.toString(), 1234));
            
            Iterator<ResourceRecordSet<?>> records = api.listByName("www.foo.com");
            
            while (records.hasNext()) {
            	ResourceRecordSet<?> record = records.next();
            	
            	assertEquals(record.getName(), "www.foo.com");
            	assertEquals(record.getTTL().get().intValue(), 600000);
            }

            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /domains/1234/records HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void listByNameWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("/");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(NOT_FOUND.getStatusCode())); // no existing records

        try {
            CloudDNSResourceRecordSetApi api = new CloudDNSResourceRecordSetApi(mockCloudDNSApi(url.toString(), 1234));
            
            assertFalse(api.listByName("www.foo.com").hasNext());
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /domains/1234/records HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    String recordsByNameAndType = "{\"records\":[{\"name\":\"www.foo.com\",\"id\":\"A-9872761\",\"type\":\"A\",\"data\":\"1.2.3.4\",\"ttl\":600000,\"updated\":\"2013-04-13T14:42:00.000+0000\",\"created\":\"2013-04-13T14:42:00.000+0000\"},{\"name\":\"www.foo.com\",\"id\":\"A-9883329\",\"type\":\"A\",\"data\":\"5.6.7.8\",\"ttl\":600000,\"updated\":\"2013-04-16T22:09:09.000+0000\",\"created\":\"2013-04-16T22:09:09.000+0000\"}]}";

    @Test
    public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("/");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(recordsByNameAndType));

        try {
            CloudDNSResourceRecordSetApi api = new CloudDNSResourceRecordSetApi(mockCloudDNSApi(url.toString(), 1234));
            
            assertEquals(api.getByNameAndType("www.foo.com", "A").get(),
                    a("www.foo.com", 600000, ImmutableList.of("1.2.3.4", "5.6.7.8")));
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /domains/1234/records?name=www.foo.com&type=A HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("/");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(NOT_FOUND.getStatusCode())); // no existing records
        
        try {
            CloudDNSResourceRecordSetApi api = new CloudDNSResourceRecordSetApi(mockCloudDNSApi(url.toString(), 1234));
            
            assertEquals(api.getByNameAndType("www.foo.com", "A"), Optional.absent());
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /domains/1234/records?name=www.foo.com&type=A HTTP/1.1");
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
 }
