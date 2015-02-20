package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class DiscoveryDNSCheckConnectionApiMockTest {

  MockDiscoveryDNSServer server;

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

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDiscoveryDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
