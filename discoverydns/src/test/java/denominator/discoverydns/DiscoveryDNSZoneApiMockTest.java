package denominator.discoverydns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;

public class DiscoveryDNSZoneApiMockTest {

  @Rule
  public MockDiscoveryDNSServer server = new MockDiscoveryDNSServer();

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
    assertThat(api.iterator()).isEmpty();

    server.assertRequest().hasMethod("GET").hasPath("/zones");
  }
}
