package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class DiscoveryDNSZoneApiMockTest {

  MockDiscoveryDNSServer server;

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "{ \"zones\": { \"@uri\": \"https://api.discoverydns.com/zones\", \"zoneList\": [ { \"@uri\": \"https://api.discoverydns.com/zones/123-123-123-123-123\", \"id\": \"123-123-123-123-123\", \"name\": \"denominator.io.\" } ] } }"));

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains.next())
        .hasName("denominator.io.")
        .hasId("123-123-123-123-123");

    server.assertRequest().hasMethod("GET").hasPath("/zones");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "{ \"zones\": { \"@uri\": \"https://api.discoverydns.com/zones\", \"zoneList\": [ ] } }"));

    ZoneApi api = server.connect().api().zones();
    assertFalse(api.iterator().hasNext());

    server.assertRequest().hasMethod("GET").hasPath("/zones");
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
