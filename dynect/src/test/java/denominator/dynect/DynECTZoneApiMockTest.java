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
        "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3601, \"fqdn\": \"denominator.io\", \"record_type\": \"SOA\", \"rdata\": {\"rname\": \"nil@denominator.io\", \"retry\": 600, \"mname\": \"ns1.p21.dynect.net.\", \"minimum\": 1800, \"refresh\": 3601, \"expire\": 604800, \"serial\": 478}, \"record_id\": 154671809, \"serial_style\": \"increment\"}], \"job_id\": 1548708326, \"msgs\": [{\"INFO\": \"detail: Found 1 record\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"
    ));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("denominator.io", "denominator.io", 3601, "nil@denominator.io")
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
        "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3601, \"fqdn\": \"denominator.io\", \"record_type\": \"SOA\", \"rdata\": {\"rname\": \"nil@denominator.io\", \"retry\": 600, \"mname\": \"ns1.p21.dynect.net.\", \"minimum\": 1800, \"refresh\": 3601, \"expire\": 604800, \"serial\": 478}, \"record_id\": 154671809, \"serial_style\": \"increment\"}], \"job_id\": 1548708326, \"msgs\": [{\"INFO\": \"detail: Found 1 record\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"
    ));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.create("denominator.io.", "denominator.io.", 3601, "nil@denominator.io")
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

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/Zone/denominator.io.")
        .hasBody("{\"ttl\":3601,\"rname\":\"nil@denominator.io\"}");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io.")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(400).setBody("{\n"
                                                                   + "  \"status\": \"failure\",\n"
                                                                   + "  \"data\": {},\n"
                                                                   + "  \"job_id\": 1567063312,\n"
                                                                   + "  \"msgs\": [\n"
                                                                   + "    {\n"
                                                                   + "      \"INFO\": \"name: Name already exists\",\n"
                                                                   + "      \"SOURCE\": \"BLL\",\n"
                                                                   + "      \"ERR_CD\": \"TARGET_EXISTS\",\n"
                                                                   + "      \"LVL\": \"ERROR\"\n"
                                                                   + "    },\n"
                                                                   + "    {\n"
                                                                   + "      \"INFO\": \"create: You already have this zone.\",\n"
                                                                   + "      \"SOURCE\": \"BLL\",\n"
                                                                   + "      \"ERR_CD\": null,\n"
                                                                   + "      \"LVL\": \"INFO\"\n"
                                                                   + "    }\n"
                                                                   + "  ]\n"
                                                                   + "}"));
    server.enqueue(new MockResponse().setBody("{\n"
                                              + "  \"status\": \"success\",\n"
                                              + "  \"data\": [\n"
                                              + "    {\n"
                                              + "      \"zone\": \"denominator.io\",\n"
                                              + "      \"ttl\": 3601,\n"
                                              + "      \"fqdn\": \"denominator.io\",\n"
                                              + "      \"record_type\": \"SOA\",\n"
                                              + "      \"rdata\": {\n"
                                              + "        \"rname\": \"test@denominator.io.\",\n"
                                              + "        \"retry\": 600,\n"
                                              + "        \"mname\": \"ns1.p21.dynect.net.\",\n"
                                              + "        \"minimum\": 1800,\n"
                                              + "        \"refresh\": 3600,\n"
                                              + "        \"expire\": 604800,\n"
                                              + "        \"serial\": 1\n"
                                              + "      },\n"
                                              + "      \"record_id\": 156835957,\n"
                                              + "      \"serial_style\": \"increment\"\n"
                                              + "    }\n"
                                              + "  ],\n"
                                              + "  \"job_id\": 1567063318,\n"
                                              + "  \"msgs\": [\n"
                                              + "    {\n"
                                              + "      \"INFO\": \"detail: Found 1 record\",\n"
                                              + "      \"SOURCE\": \"BLL\",\n"
                                              + "      \"ERR_CD\": null,\n"
                                              + "      \"LVL\": \"INFO\"\n"
                                              + "    }\n"
                                              + "  ]\n"
                                              + "}\n"));
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/Zone/denominator.io.")
        .hasBody("{\"ttl\":3601,\"rname\":\"nil@denominator.io\"}");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/SOARecord/denominator.io./denominator.io.?detail=Y");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/SOARecord/denominator.io./denominator.io./156835957")
        .hasBody("{\"ttl\":\"3601\",\"rdata\":{\"rname\":\"nil@denominator.io\"}}");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io.")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/Zone/denominator.io.");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\n"
        + "  \"status\": \"failure\",\n"
        + "  \"data\": {},\n"
        + "  \"job_id\": 1567063448,\n"
        + "  \"msgs\": [\n"
        + "    {\n"
        + "      \"INFO\": \"zone: No such zone\",\n"
        + "      \"SOURCE\": \"BLL\",\n"
        + "      \"ERR_CD\": \"NOT_FOUND\",\n"
        + "      \"LVL\": \"ERROR\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"INFO\": \"remove: No such zone in your account\",\n"
        + "      \"SOURCE\": \"BLL\",\n"
        + "      \"ERR_CD\": null,\n"
        + "      \"LVL\": \"INFO\"\n"
        + "    }\n"
        + "  ]\n"
        + "}"));

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/Zone/denominator.io.");
  }
}
