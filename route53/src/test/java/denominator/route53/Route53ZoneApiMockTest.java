package denominator.route53;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.route53.Route53Test.hostedZones;
import static denominator.route53.Route53Test.noHostedZones;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class Route53ZoneApiMockTest {

  static ZoneApi mockApi(final int port) {
    return Denominator.create(new Route53Provider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials("accessKey", "secretKey")).api().zones();
  }

  @Test
  public void iteratorWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(hostedZones));
    server.play();

    try {
      ZoneApi api = mockApi(server.getPort());
      Zone zone = api.iterator().next();
      assertEquals(zone.name(), "denominator.io.");
      assertEquals(zone.id(), "Z1PA6795UKMFR9");

      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "GET /2012-12-12/hostedzone HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iteratorWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(noHostedZones));
    server.play();

    try {
      ZoneApi api = mockApi(server.getPort());
      assertFalse(api.iterator().hasNext());

      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "GET /2012-12-12/hostedzone HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }
}
