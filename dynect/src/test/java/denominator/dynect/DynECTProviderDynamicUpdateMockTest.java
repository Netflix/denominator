package denominator.dynect;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.dynect.DynECTTest.noneWithNameAndType;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test(singleThreaded = true)
public class DynECTProviderDynamicUpdateMockTest {

  static String
      session =
      "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.5.0\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

  static String
      mismatch =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 305900967, \"msgs\": [{\"INFO\": \"login: IP address does not match current session\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"INVALID_DATA\", \"LVL\": \"ERROR\"}, {\"INFO\": \"login: There was a problem with your credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

  static String
      sessionValid =
      "{\"status\": \"success\", \"data\": {}, \"job_id\": 427274293, \"msgs\": [{\"INFO\": \"isalive: User session is still active\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

  static String
      badSession =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 427275274, \"msgs\": [{\"INFO\": \"login: Bad or expired credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"INVALID_DATA\", \"LVL\": \"ERROR\"}, {\"INFO\": \"login: There was a problem with your credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

  @Test
  public void ipMisMatchInvalidatesAndRetries() throws IOException, InterruptedException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(mismatch));
    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
    server.play();

    try {

      DNSApi api = Denominator.create(new DynECTProvider() {
        @Override
        public String url() {
          return "http://localhost:" + server.getPort();
        }
      }, credentials("customer", "joe", "letmein")).api();

      assertNull(
          api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));

      assertEquals(server.getRequestCount(), 4);
      assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void dynamicEndpointUpdates() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
    server.play();

    try {
      String initialPath = "";
      String updatedPath = "/alt";
      URL mockUrl = server.getUrl(initialPath);
      final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);

      DNSApi api = Denominator.create(new DynECTProvider() {
        @Override
        public String url() {
          return dynamicUrl.get().toString();
        }
      }, credentials("customer", "joe", "letmein")).api();

      assertNull(
          api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));
      dynamicUrl.set(new URL(mockUrl, updatedPath));
      assertNull(
          api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));

      assertEquals(server.getRequestCount(), 4);
      assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "POST /alt/Session HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /alt/ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void dynamicCredentialUpdates() throws IOException, InterruptedException {
    final MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
    server.enqueue(new MockResponse().setBody(session));
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
    server.play();

    try {
      AtomicReference<Credentials>
          dynamicCredentials =
          new AtomicReference<Credentials>(ListCredentials.from(
              "customer", "joe", "letmein"));

      DNSApi api = Denominator.create(new DynECTProvider() {
        @Override
        public String url() {
          return "http://localhost:" + server.getPort();
        }
      }, new OverrideCredentials(dynamicCredentials)).api();

      assertNull(
          api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));
      dynamicCredentials.set(ListCredentials.from("customer2", "bob", "comeon"));
      assertNull(
          api.basicRecordSetsInZone("denominator.io").getByNameAndType("www.denominator.io", "A"));

      assertEquals(server.getRequestCount(), 4);
      assertEquals(new String(server.takeRequest().getBody()),
                   "{\"customer_name\":\"customer\",\"user_name\":\"joe\",\"password\":\"letmein\"}");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
      assertEquals(new String(server.takeRequest().getBody()),
                   "{\"customer_name\":\"customer2\",\"user_name\":\"bob\",\"password\":\"comeon\"}");
      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
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
