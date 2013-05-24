package denominator.dynect;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

@Test(singleThreaded = true)
public class DynECTProviderDynamicUpdateMockTest {

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.3.8\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void dynamicEndpointUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            String initialPath = "/";
            String updatedPath = "/alt/";
            URL mockUrl = server.getUrl(initialPath);
            final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);

            DNSApi api = Denominator.create(new DynECTProvider() {
                @Override
                public String getUrl() {
                    return dynamicUrl.get().toString();
                }
            }, credentials("customer", "joe", "letmein")).getApi();

            assertEquals(api.getResourceRecordSetApiForZone("denominator.io").getByNameAndType("www.denominator.io", "A"), Optional.absent());
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertEquals(api.getResourceRecordSetApiForZone("denominator.io").getByNameAndType("www.denominator.io", "A"), Optional.absent());

            assertEquals(server.getRequestCount(), 3);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /alt/ARecord/denominator.io/www.denominator.io HTTP/1.1");
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

            DNSApi api = Denominator.create(new DynECTProvider() {
                @Override
                public String getUrl() {
                    return server.getUrl("/").toString();
                }
            }, credentials(new Supplier<Credentials>(){
                @Override
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            })).getApi();

            assertEquals(api.getResourceRecordSetApiForZone("denominator.io").getByNameAndType("www.denominator.io", "A"), Optional.absent());
            dynamicCredentials.set(ListCredentials.from("customer2", "bob", "comeon"));
            assertEquals(api.getResourceRecordSetApiForZone("denominator.io").getByNameAndType("www.denominator.io", "A"), Optional.absent());

            assertEquals(server.getRequestCount(), 4);
            assertEquals(new String(server.takeRequest().getBody()), "{\"customer_name\":\"customer\",\"user_name\":\"joe\",\"password\":\"letmein\"}");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(new String(server.takeRequest().getBody()), "{\"customer_name\":\"customer2\",\"user_name\":\"bob\",\"password\":\"comeon\"}");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }
}
