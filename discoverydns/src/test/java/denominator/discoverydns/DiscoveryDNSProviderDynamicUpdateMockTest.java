package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import denominator.DNSApi;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;

public class DiscoveryDNSProviderDynamicUpdateMockTest {

  @Rule
  public MockDiscoveryDNSServer server = new MockDiscoveryDNSServer();

  String
      zones =
      "{ \"zones\": { \"@uri\": \"https://api.discoverydns.com/zones\", \"zoneList\": [ ] } }";

  @Test
  public void dynamicEndpointUpdates() throws Exception {
    final AtomicReference<String> url = new AtomicReference<String>(server.url());
    server.enqueue(new MockResponse().setBody(zones));

    DNSApi api = Denominator.create(new DiscoveryDNSProvider() {
      @Override
      public String url() {
        return url.get();
      }
    }, credentials(server.credentials())).api();

    api.zones().iterator();
    server.assertRequest().hasPath("/zones");

    url.set(server.url() + "/newPath");
    server.enqueue(new MockResponse().setBody(zones));

    api.zones().iterator();

    server.assertRequest().hasPath("/newPath/zones");
  }
}
