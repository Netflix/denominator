package denominator.designate;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.designate.DesignateTest.accessResponse;
import static denominator.designate.DesignateTest.domainId;
import static denominator.designate.DesignateTest.domainsResponse;
import static denominator.designate.DesignateTest.getURLReplacingQueueDispatcher;
import static denominator.designate.DesignateTest.password;
import static denominator.designate.DesignateTest.takeAuthResponse;
import static denominator.designate.DesignateTest.tenantId;
import static denominator.designate.DesignateTest.username;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class DesignateZoneApiMockTest {

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setBody(accessResponse));
        server.enqueue(new MockResponse().setBody(domainsResponse));

        try {
            ZoneApi api = mockApi(url);
            Iterator<Zone> domains = api.iterator();

            while (domains.hasNext()) {
                Zone zone = domains.next();
                assertEquals(zone.name(), "denominator.io.");
                assertEquals(zone.id(), domainId);
            }

            assertEquals(server.getRequestCount(), 2);
            takeAuthResponse(server);
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.play();

        URL url = server.getUrl("");
        server.setDispatcher(getURLReplacingQueueDispatcher(url));

        server.enqueue(new MockResponse().setBody(accessResponse));
        server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

        try {
            ZoneApi api = mockApi(url);

            assertFalse(api.iterator().hasNext());
            assertEquals(server.getRequestCount(), 2);
            takeAuthResponse(server);
            assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static ZoneApi mockApi(final URL url) {
        return Denominator.create(new DesignateProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials(tenantId, username, password)).api().zones();
    }
}
