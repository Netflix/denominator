package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatus;
import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatusResponse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class NetworkStatusReadableMockTest {

  static DNSApiManager mockApi(final int port) {
    return Denominator.create(new UltraDNSProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials("joe", "letmein"));
  }

  @Test
  public void singleRequestOnSuccess() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();

    server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));
    server.play();

    try {
      assertTrue(mockApi(server.getPort()).checkConnection());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void singleRequestOnFailure() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();

    server.enqueue(new MockResponse().setResponseCode(500));
    server.play();

    try {
      assertFalse(mockApi(server.getPort()).checkConnection());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
    } finally {
      server.shutdown();
    }
  }
}
