package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

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

@Test(singleThreaded = true)
public class DesignateZoneApiMockTest {

  private static ZoneApi mockApi(final int port) {
    return Denominator.create(new DesignateProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials(tenantId, username, password)).api().zones();
  }

  @Test
  public void iteratorWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(domainsResponse));

    try {
      ZoneApi api = mockApi(server.getPort());
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

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    try {
      ZoneApi api = mockApi(server.getPort());

      assertFalse(api.iterator().hasNext());
      assertEquals(server.getRequestCount(), 2);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }
}
