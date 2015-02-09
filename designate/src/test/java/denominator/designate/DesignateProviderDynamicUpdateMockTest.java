package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
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

@Test(singleThreaded = true)
public class DesignateProviderDynamicUpdateMockTest {

  MockDesignateServer server;

  @Test
  public void dynamicEndpointUpdates() throws Exception {
    final AtomicReference<String> url = new AtomicReference<String>(server.url());
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    DNSApi api = Denominator.create(new DesignateProvider() {
      @Override
      public String url() {
        return url.get();
      }
    }, credentials(server.credentials())).api();

    api.zones().iterator();
    server.assertAuthRequest();
    server.assertRequest();

    MockDesignateServer server2 = new MockDesignateServer();
    url.set(server2.url());
    server2.enqueueAuthResponse();
    server2.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    api.zones().iterator();

    server2.assertAuthRequest();
    server2.assertRequest();
    server2.shutdown();
  }

  @Test
  public void dynamicCredentialUpdates() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    AtomicReference<Credentials>
        dynamicCredentials =
        new AtomicReference<Credentials>(server.credentials());

    DNSApi
        api =
        Denominator.create(server, new OverrideCredentials(dynamicCredentials)).api();

    api.zones().iterator();

    server.assertAuthRequest();
    server.assertRequest();

    dynamicCredentials.set(ListCredentials.from("tim", "jclouds-bob", "comeon"));

    server.credentials("tim", "jclouds-bob", "comeon");
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    api.zones().iterator();

    server.assertAuthRequest();
    server.assertRequest();
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

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDesignateServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
