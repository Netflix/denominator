package denominator.clouddns;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.clouddns.CloudDNSZoneApiMockTest.getURLReplacingQueueDispatcher;
import static denominator.clouddns.CloudDNSZoneApiMockTest.session;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class LimitsReadableMockTest {

  String
      limits =
      "{ \"limits\" : { \"rate\" : [{\"regex\" : \".*/v\\d+\\.\\d+/(\\d+/status).*\",\"uri\" : \"*/status/*\", \"limit\" : [{\"next-available\" : \"2013-07-27T20:00:57.671Z\",\"unit\" : \"SECOND\",\"remaining\" : 5,\"value\" : 5,\"verb\" : \"GET\"}]},{\"regex\" : \".*/v\\d+\\.\\d+/(\\d+/domains).*\",\"uri\" : \"*/domains*\", \"limit\" : [{\"next-available\" : \"2013-07-27T20:00:57.671Z\",\"unit\" : \"MINUTE\",\"remaining\" : 100,\"value\" : 100,\"verb\" : \"GET\"},{\"next-available\" : \"2013-07-27T20:00:57.671Z\",\"unit\" : \"MINUTE\",\"remaining\" : 25,\"value\" : 25,\"verb\" : \"POST\"},{\"next-available\" : \"2013-07-27T20:00:57.671Z\",\"unit\" : \"MINUTE\",\"remaining\" : 50,\"value\" : 50,\"verb\" : \"PUT\"},{\"next-available\" : \"2013-07-27T20:00:57.671Z\",\"unit\" : \"MINUTE\",\"remaining\" : 50,\"value\" : 50,\"verb\" : \"DELETE\"}]}],\"absolute\" : {\"domains\" : 500,\"records per domain\" : 500}}}";

  private static DNSApiManager mockApi(final String url) {
    return Denominator.create(new CloudDNSProvider() {
      @Override
      public String url() {
        return url;
      }
    }, credentials("jclouds-joe", "letmein"));
  }

  @Test
  public void implicitlyStartsSessionWhichIsReusedForLaterRequests()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String url = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(url));

    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setBody(limits));
    server.enqueue(new MockResponse().setBody(limits));

    try {
      DNSApiManager api = mockApi("http://localhost:" + server.getPort());
      assertTrue(api.checkConnection());
      assertTrue(api.checkConnection());

      assertEquals(server.getRequestCount(), 3);
      assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/limits HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1.0/123123/limits HTTP/1.1");
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
      assertFalse(mockApi("http://localhost:" + server.getPort()).checkConnection());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }
}
