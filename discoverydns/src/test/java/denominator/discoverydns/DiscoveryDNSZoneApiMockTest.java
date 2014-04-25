package denominator.discoverydns;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class DiscoveryDNSZoneApiMockTest {

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.zones));
        server.play();

        try {
            ZoneApi api = DiscoveryDNSTest.mockApi(server.getPort()).api().zones();
            Zone zone = api.iterator().next();
            assertEquals(server.getRequestCount(), 1);
            assertEquals(server.takeRequest().getRequestLine(), "GET /zones HTTP/1.1");
            assertEquals(zone.name(), "denominator.io.");
            assertEquals(zone.id(), "123-123-123-123-123");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.noZones));
        server.play();

        try {
            ZoneApi api = DiscoveryDNSTest.mockApi(server.getPort()).api().zones();
            assertFalse(api.iterator().hasNext());
            assertEquals(server.getRequestCount(), 1);
            assertEquals(server.takeRequest().getRequestLine(), "GET /zones HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }
}
