package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.dynect.DynECTTest.noZones;
import static denominator.dynect.DynECTTest.zones;

public class DynECTZoneApiMockTest {

  @Rule
  public MockDynECTServer server = new MockDynECTServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(zones));

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains.next())
        .hasName("0.0.0.0.d.6.e.0.0.a.2.ip6.arpa");
    assertThat(domains.next())
        .hasName("126.12.44.in-addr.arpa");
    assertThat(domains.next())
        .hasName("denominator.io");
    assertThat(domains).isEmpty();

    server.assertSessionRequest();
    server.assertRequest().hasPath("/Zone");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(noZones));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();

    server.assertSessionRequest();
    server.assertRequest().hasPath("/Zone");
  }

  @Test
  public void iteratorByNameWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(
        "{\"status\": \"success\", \"data\": {\"zone_type\": \"Primary\", \"serial_style\": \"increment\", \"serial\": 1, \"zone\": \"denominator.io\"}, \"job_id\": 1536811990, \"msgs\": [{\"INFO\": \"get: Your zone, denominator.io\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io."))
        .contains(Zone.create("denominator.io."));

    server.assertSessionRequest();
    server.assertRequest().hasPath("/Zone/denominator.io.");
  }

  @Test
  public void iteratorByNameWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"status\": \"failure\", \"data\": {}, \"job_id\": 1534838771, \"msgs\": [{\"INFO\": \"zone: No such zone\", \"SOURCE\": \"API-B\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}]}"));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSessionRequest();
    server.assertRequest().hasPath("/Zone/denominator.io.");
  }
}
