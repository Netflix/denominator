package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class DiscoveryDNSCheckConnectionApiMockTest {

  @Test
  public void success() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.users));
    server.play();

    try {
      DNSApiManager api = DiscoveryDNSTest.mockApi(server.getPort());
      assertTrue(api.checkConnection());
      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "GET /users HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void unsuccessful() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(DiscoveryDNSTest.noUsers));
    server.play();

    try {
      DNSApiManager api = DiscoveryDNSTest.mockApi(server.getPort());
      assertFalse(api.checkConnection());
      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "GET /users HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }
}
