package denominator.designate;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.designate.DesignateTest.accessResponse;
import static denominator.designate.DesignateTest.auth;
import static denominator.designate.DesignateTest.getURLReplacingQueueDispatcher;
import static denominator.designate.DesignateTest.password;
import static denominator.designate.DesignateTest.tenantId;
import static denominator.designate.DesignateTest.username;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

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
public class DesignateProviderDynamicUpdateMockTest {

    @Test
    public void dynamicEndpointUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        String initialPath = "";
        String updatedPath = "alt";
        URL mockUrl = server.getUrl(initialPath);
        final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);
        server.setDispatcher(getURLReplacingQueueDispatcher(dynamicUrl));

        server.enqueue(new MockResponse().setBody(accessResponse));
        server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));
        server.enqueue(new MockResponse().setBody(accessResponse));
        server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

        try {
            DNSApi api = Denominator.create(new DesignateProvider() {
                @Override
                public String url() {
                    return dynamicUrl.get().toString();
                }
            }, credentials(tenantId, username, password)).api();

            assertFalse(api.zones().iterator().hasNext());
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertFalse(api.zones().iterator().hasNext());

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "POST /alt/tokens HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /alt/v1/domains HTTP/1.1");
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

        server.enqueue(new MockResponse().setBody(accessResponse));
        server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));
        server.enqueue(new MockResponse().setBody(accessResponse));
        server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

        try {

            final AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(
                    ListCredentials.from(tenantId, username, password));

            @Module(complete = false, library = true, overrides = true)
            class OverrideCredentials {
                @Provides
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            }

            DNSApi api = Denominator.create(new DesignateProvider() {
                @Override
                public String url() {
                    return mockUrl.toString();
                }
            }, new OverrideCredentials()).api();

            assertFalse(api.zones().iterator().hasNext());
            dynamicCredentials.set(ListCredentials.from(tenantId, "jclouds-bob", "comeon"));
            assertFalse(api.zones().iterator().hasNext());

            assertEquals(server.getRequestCount(), 4);
            assertEquals(new String(server.takeRequest().getBody()), auth);
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
            assertEquals(new String(server.takeRequest().getBody()),
                    auth.replace(username, "jclouds-bob").replace(password, "comeon"));
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }
}
