package denominator.dynect;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.badSession;
import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.session;
import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.sessionValid;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class InvalidatableTokenProviderMockTest {

  private static DNSApiManager mockApi(final int port) {
    return Denominator.create(new DynECTProvider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials("jclouds", "joe", "letmein"));
  }

  @Test
  public void successThenFailure() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setBody(sessionValid));
    server.enqueue(new MockResponse().setBody(sessionValid));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(badSession));

    try {
      DNSApiManager api = mockApi(server.getPort());

      assertTrue(api.checkConnection());
      assertTrue(api.checkConnection());
      assertFalse(api.checkConnection());

      assertEquals(server.getRequestCount(), 4);
      assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /Session HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void singleRequestOnFailure() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    server.enqueue(new MockResponse().setResponseCode(401));

    try {
      assertFalse(mockApi(server.getPort()).checkConnection());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }
}
