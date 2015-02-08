package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.designate.DesignateTest.accessResponse;
import static denominator.designate.DesignateTest.auth;
import static denominator.designate.DesignateTest.getURLReplacingQueueDispatcher;
import static denominator.designate.DesignateTest.password;
import static denominator.designate.DesignateTest.tenantId;
import static denominator.designate.DesignateTest.username;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class DesignateProviderDynamicUpdateMockTest {

  @Test
  public void dynamicEndpointUpdates() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    String updatedPath = "alt";
    String mockUrl = "http://localhost:" + server.getPort();
    final AtomicReference<String> dynamicUrl = new AtomicReference<String>(mockUrl);
    server.setDispatcher(getURLReplacingQueueDispatcher(dynamicUrl));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));
    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    try {
      DNSApi api = Denominator.create(new DesignateProvider() {
        @Override
        public String url() {
          return dynamicUrl.get().toString();
        }
      }, credentials(tenantId, username, password)).api();

      assertFalse(api.zones().iterator().hasNext());
      dynamicUrl.set(dynamicUrl.get() + "/" + updatedPath);
      assertFalse(api.zones().iterator().hasNext());

      assertEquals(server.getRequestCount(), 4);
      assertEquals(server.takeRequest().getRequestLine(), "POST /tokens HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "POST /alt/tokens HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /alt/v1/domains HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void dynamicCredentialUpdates() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.play();

    final String mockUrl = "http://localhost:" + server.getPort();
    server.setDispatcher(getURLReplacingQueueDispatcher(new AtomicReference<String>(mockUrl)));

    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));
    server.enqueue(new MockResponse().setBody(accessResponse));
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    try {
      AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(
          ListCredentials.from(tenantId, username, password));

      DNSApi api = Denominator.create(new DesignateProvider() {
        @Override
        public String url() {
          return mockUrl;
        }
      }, new OverrideCredentials(dynamicCredentials)).api();

      assertFalse(api.zones().iterator().hasNext());
      dynamicCredentials.set(ListCredentials.from(tenantId, "jclouds-bob", "comeon"));
      assertFalse(api.zones().iterator().hasNext());

      assertEquals(server.getRequestCount(), 4);
      assertEquals(new String(server.takeRequest().getBody()), auth);
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
      assertEquals(new String(server.takeRequest().getBody()),
                   auth.replace(username, "jclouds-bob").replace(password, "comeon"));
      assertEquals(server.takeRequest().getRequestLine(), "GET /v1/domains HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Module(complete = false, library = true, overrides = true)
  static class OverrideCredentials {

    final AtomicReference<Credentials> dynamicCredentials;

    OverrideCredentials(AtomicReference<Credentials> dynamicCredentials) {
      this.dynamicCredentials = dynamicCredentials;
    }

    @Provides
    public Credentials get() {
      return dynamicCredentials.get();
    }
  }
}
