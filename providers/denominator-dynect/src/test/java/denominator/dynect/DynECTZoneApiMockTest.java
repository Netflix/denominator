package denominator.dynect;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.session;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class DynECTZoneApiMockTest {

    String zones ="{\"status\": \"success\", \"data\": [\"/REST/Zone/0.0.0.0.d.6.e.0.0.a.2.ip6.arpa/\", \"/REST/Zone/126.12.44.in-addr.arpa/\", \"/REST/Zone/jclouds.org/\"], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 3 zones\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(zones));
        server.play();

        try {
            ZoneApi api = mockApi(server.getUrl(""));
            Zone zone = api.iterator().next();
            assertEquals(zone.name(), "0.0.0.0.d.6.e.0.0.a.2.ip6.arpa");
            assertFalse(zone.id() != null);

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    String noZones ="{\"status\": \"success\", \"data\": [], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 0 zones\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(noZones));
        server.play();

        try {
            ZoneApi api = mockApi(server.getUrl(""));
            assertFalse(api.iterator().hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static ZoneApi mockApi(final URL url) {
        return Denominator.create(new DynECTProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("jclouds", "joe", "letmein")).api().zones();
    }
 }
