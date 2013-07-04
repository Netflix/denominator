package denominator.dynect;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

@Test(singleThreaded = true)
public class DynECTProviderDynamicUpdateMockTest {

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.5.0\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String mismatch = "{\"status\": \"failure\", \"data\": {}, \"job_id\": 305900967, \"msgs\": [{\"INFO\": \"login: IP address does not match current session\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"INVALID_DATA\", \"LVL\": \"ERROR\"}, {\"INFO\": \"login: There was a problem with your credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void ipMisMatchInvalidatesAndRetries() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setBody(mismatch));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {

            DNSApi api = Denominator.create(new DynECTProvider() {
                @Override
                public String url() {
                    return server.getUrl("").toString();
                }
            }, credentials("customer", "joe", "letmein")).api();

            assertNull(api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicEndpointUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            String initialPath = "";
            String updatedPath = "/alt";
            URL mockUrl = server.getUrl(initialPath);
            final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);

            DNSApi api = Denominator.create(new DynECTProvider() {
                @Override
                public String url() {
                    return dynamicUrl.get().toString();
                }
            }, credentials("customer", "joe", "letmein")).api();

            assertNull(api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertNull(api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "POST /alt/Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /alt/ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicCredentialUpdates() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            final AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(ListCredentials.from("customer", "joe", "letmein"));

            @Module(complete = false, library = true, overrides = true)
            class OverrideCredentials {
                @Provides
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            }

            DNSApi api = Denominator.create(new DynECTProvider() {
                @Override
                public String url() {
                    return server.getUrl("").toString();
                }
            }, new OverrideCredentials()).api();

            assertNull(api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));
            dynamicCredentials.set(ListCredentials.from("customer2", "bob", "comeon"));
            assertNull(api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(new String(server.takeRequest().getBody()), "{\"customer_name\":\"customer\",\"user_name\":\"joe\",\"password\":\"letmein\"}");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(new String(server.takeRequest().getBody()), "{\"customer_name\":\"customer2\",\"user_name\":\"bob\",\"password\":\"comeon\"}");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }
}
