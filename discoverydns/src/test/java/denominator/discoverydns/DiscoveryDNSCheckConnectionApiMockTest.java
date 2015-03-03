package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.DNSApiManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscoveryDNSCheckConnectionApiMockTest {

  @Rule
  public MockDiscoveryDNSServer server = new MockDiscoveryDNSServer();

  @Test
  public void success() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "{ \"users\": { \"@uri\": \"https://api.discoverydns.com/users\", \"userList\": [ { \"id\": \"123-123-123-123-123\" } ] } }"));

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());

    server.assertRequest().hasMethod("GET").hasPath("/users");
  }

  @Test
  public void unsuccessful() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "{ \"users\": { \"@uri\": \"https://api.discoverydns.com/users\", \"userList\": [ ] } }"));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertRequest().hasMethod("GET").hasPath("/users");
  }
}
