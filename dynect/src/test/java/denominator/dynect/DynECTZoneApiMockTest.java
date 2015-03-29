package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

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
    server.enqueue(new MockResponse().setBody(
        "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"denominator.io\", \"record_type\": \"SOA\", \"rdata\": {\"rname\": \"fake@denominator.io.\", \"retry\": 600, \"mname\": \"ns1.p21.dynect.net.\", \"minimum\": 1800, \"refresh\": 3600, \"expire\": 604800, \"serial\": 478}, \"record_id\": 154671809, \"serial_style\": \"increment\"}], \"job_id\": 1548708326, \"msgs\": [{\"INFO\": \"detail: Found 1 record\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"
    ));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.builder().name("denominator.io").id("denominator.io").email("fake@denominator.io.")
            .ttl(1800).build()
    );

    server.assertSessionRequest();
    server.assertRequest().hasPath("/Zone");
    server.assertRequest().hasPath("/SOARecord/denominator.io/denominator.io?detail=Y");
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
        "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"denominator.io\", \"record_type\": \"SOA\", \"rdata\": {\"rname\": \"fake@denominator.io.\", \"retry\": 600, \"mname\": \"ns1.p21.dynect.net.\", \"minimum\": 1800, \"refresh\": 3600, \"expire\": 604800, \"serial\": 478}, \"record_id\": 154671809, \"serial_style\": \"increment\"}], \"job_id\": 1548708326, \"msgs\": [{\"INFO\": \"detail: Found 1 record\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"
    ));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.builder().name("denominator.io.").id("denominator.io.").email("fake@denominator.io.")
            .ttl(1800).build()
    );

    server.assertSessionRequest();
    server.assertRequest().hasPath("/SOARecord/denominator.io./denominator.io.?detail=Y");
  }

  @Test
  public void iteratorByNameWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"status\": \"failure\", \"data\": {}, \"job_id\": 1534838771, \"msgs\": [{\"INFO\": \"zone: No such zone\", \"SOURCE\": \"API-B\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}]}"));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSessionRequest();
    server.assertRequest().hasPath("/SOARecord/denominator.io./denominator.io.?detail=Y");
  }
}
