package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.dynect.DynECTTest.noZones;
import static denominator.dynect.DynECTTest.zones;
import static org.assertj.core.api.Assertions.assertThat;

public class DynECTProviderDynamicUpdateMockTest {

  @Rule
  public MockDynECTServer server = new MockDynECTServer();

  @Test
  public void ipMisMatchInvalidatesAndRetries() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(
        "{\"status\": \"failure\", \"data\": {}, \"job_id\": 305900967, \"msgs\": [{\"INFO\": \"login: IP address does not match current session\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"INVALID_DATA\", \"LVL\": \"ERROR\"}, {\"INFO\": \"login: There was a problem with your credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"));
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(noZones));

    DNSApi api = server.connect().api();

    assertThat(api.zones()).isEmpty();

    server.assertSessionRequest();
    server.assertRequest();
    server.assertSessionRequest();
    server.assertRequest();
  }

  @Test
  public void dynamicEndpointUpdates() throws Exception {
    final AtomicReference<String> url = new AtomicReference<String>(server.url());
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(zones));

    DNSApi api = Denominator.create(new DynECTProvider() {
      @Override
      public String url() {
        return url.get();
      }
    }, credentials(server.credentials())).api();

    api.zones().iterator();
    server.assertSessionRequest();
    server.assertRequest();

    MockDynECTServer server2 = new MockDynECTServer();
    url.set(server2.url());
    server2.enqueueSessionResponse();
    server2.enqueue(new MockResponse().setBody(zones));

    api.zones().iterator();

    server2.assertSessionRequest();
    server2.assertRequest();
    server2.shutdown();
  }

  @Test
  public void dynamicCredentialUpdates() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(zones));

    AtomicReference<Credentials>
        dynamicCredentials =
        new AtomicReference<Credentials>(server.credentials());

    DNSApi
        api =
        Denominator.create(server, new OverrideCredentials(dynamicCredentials)).api();

    api.zones().iterator();

    server.assertSessionRequest();
    server.assertRequest();

    dynamicCredentials.set(ListCredentials.from("tim", "jclouds-bob", "comeon"));

    server.credentials("tim", "jclouds-bob", "comeon");
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(zones));

    api.zones().iterator();

    server.assertSessionRequest();
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

  static String
      sessionValid =
      "{\"status\": \"success\", \"data\": {}, \"job_id\": 427274293, \"msgs\": [{\"INFO\": \"isalive: User session is still active\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
}
