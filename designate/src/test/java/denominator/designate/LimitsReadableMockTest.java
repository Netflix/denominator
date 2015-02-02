package denominator.designate;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.designate.DesignateTest.accessResponse;
import static denominator.designate.DesignateTest.getURLReplacingQueueDispatcher;
import static denominator.designate.DesignateTest.limitsResponse;
import static denominator.designate.DesignateTest.password;
import static denominator.designate.DesignateTest.takeAuthResponse;
import static denominator.designate.DesignateTest.tenantId;
import static denominator.designate.DesignateTest.username;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class LimitsReadableMockTest {

  private static DNSApiManager mockApi(final int port) {
    return Denominator.create(new DesignateProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials(tenantId, username, password));
  }

  @Test
  public void implicitlyStartsSessionWhichIsReusedForLaterRequests()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody(limitsResponse));
    server.enqueue(new MockResponse().setBody(limitsResponse));

    try {
      DNSApiManager api = mockApi(server.getPort());

      assertTrue(api.checkConnection());
      assertTrue(api.checkConnection());

      assertEquals(server.getRequestCount(), 3);
      takeAuthResponse(server);
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1/limits HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1/limits HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void singleRequestOnFailure() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setResponseCode(401));

    try {
      assertFalse(mockApi(server.getPort()).checkConnection());

      assertEquals(server.getRequestCount(), 1);
      takeAuthResponse(server);
    } finally {
      server.shutdown();
    }
  }
}
