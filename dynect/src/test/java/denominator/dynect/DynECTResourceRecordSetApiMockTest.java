package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;

import denominator.ResourceRecordSetApi;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.dynect.DynECTTest.noneWithName;
import static denominator.dynect.DynECTTest.noneWithNameAndType;
import static denominator.dynect.DynECTTest.serviceNS;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.ns;

public class DynECTResourceRecordSetApiMockTest {

  @Rule
  public MockDynECTServer server = new MockDynECTServer();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void putFirstRecordPostsAndPublishes() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(a("www.denominator.io", 3600, "192.0.2.1"));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/ARecord/denominator.io/www.denominator.io")
        .hasBody(createRecord1);
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void putExistingRecordDoesNothing() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records1));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(a("www.denominator.io", 3600, "192.0.2.1"));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
  }

  @Test
  public void putSecondRecordPostsRecordPublishes() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records1));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(a("www.denominator.io", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/ARecord/denominator.io/www.denominator.io")
        .hasBody(createRecord2);
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void putReplacingRecordSet() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records1));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(a("www.denominator.io", 10000000, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath("/ARecord/denominator.io/www.denominator.io/1");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/ARecord/denominator.io/www.denominator.io")
        .hasBody(createRecord1OverriddenTTL);
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/ARecord/denominator.io/www.denominator.io")
        .hasBody(createRecord2OverriddenTTL);
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void putRecordSetSkipsWhenEqual() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records1));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(a("www.denominator.io", 3600, "192.0.2.1"));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
  }

  @Test
  public void putOneLessRecordSendsDeleteAndPublish() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records1And2));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(a("www.denominator.io", 3600, "198.51.100.1"));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath("/ARecord/denominator.io/www.denominator.io/1");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io")
        .hasBody("{\"publish\":true}");
  }

  /**
   * DynECT errors if you try to delete a service record.
   */
  @Test
  public void putDoesntDeleteServiceNSRecord() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(serviceNS));
    server.enqueue(new MockResponse().setBody(success));
    server.enqueue(new MockResponse().setBody(success));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.put(ns("denominator.io", 3600, "ns1.denominator.io."));

    server.assertSessionRequest();
    server.assertRequest().hasMethod("GET")
        .hasPath("/NSRecord/denominator.io/denominator.io?detail=Y");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/NSRecord/denominator.io/denominator.io")
        .hasBody("{\n"
                 + "  \"ttl\": 3600,\n"
                 + "  \"rdata\": {\n"
                 + "    \"nsdname\": \"ns1.denominator.io.\"\n"
                 + "  }\n"
                 + "}");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    Iterator<ResourceRecordSet<?>> iterator = api.iterator();
    iterator.next();
    iterator.next();
    assertThat(iterator.next())
        .isEqualTo(a("www.denominator.io", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/AllRecord/denominator.io?detail=Y");
  }

  @Test
  public void listWhenAbsent() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("zone denominator.io not found");

    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithName));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.iterator().hasNext();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(recordsByName));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    assertThat(api.iterateByName("www.denominator.io").next())
        .isEqualTo(a("www.denominator.io", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/AllRecord/denominator.io/www.denominator.io?detail=Y");
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithName));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    assertThat(api.iterateByName("www.denominator.io")).isEmpty();

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/AllRecord/denominator.io/www.denominator.io?detail=Y");
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(records1And2));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    assertThat(api.getByNameAndType("www.denominator.io", "A"))
        .isEqualTo(a("www.denominator.io", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    assertThat(api.getByNameAndType("www.denominator.io", "A")).isNull();

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/ARecord/denominator.io/www.denominator.io?detail=Y");
  }

  @Test
  public void deleteRRSet() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(
        "{\"status\": \"success\", \"data\": {}, \"job_id\": 1548682166, \"msgs\": [{\"INFO\": \"delete: 1 records deleted\", \"SOURCE\": \"API-B\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"));
    server.enqueue(new MockResponse().setBody(success));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.deleteByNameAndType("www.denominator.io", "A");

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath("/ARecord/denominator.io/www.denominator.io");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/Zone/denominator.io")
        .hasBody("{\"publish\":true}");
  }

  @Test
  public void deleteAbsentRRSDoesNothing() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"status\": \"failure\", \"data\": {}, \"job_id\": 1548708416, \"msgs\": [{\"INFO\": \"node: Not in zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}, {\"INFO\": \"get: Host not found in this zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"
    ));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io");
    api.deleteByNameAndType("www.denominator.io", "A");

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath("/ARecord/denominator.io/www.denominator.io");
  }

  String records;
  String recordsByName;
  String
      success =
      "{\"status\": \"success\", \"data\": {}, \"job_id\": 262989027, \"msgs\": [{\"INFO\": \"thing done\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  String createRecord1 = "{\n"
                         + "  \"ttl\": 3600,\n"
                         + "  \"rdata\": {\n"
                         + "    \"address\": \"192.0.2.1\"\n"
                         + "  }\n"
                         + "}";
  String
      records1 =
      "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"192.0.2.1\"}, \"record_id\": 1}], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  String createRecord2 = "{\n"
                         + "  \"ttl\": 3600,\n"
                         + "  \"rdata\": {\n"
                         + "    \"address\": \"198.51.100.1\"\n"
                         + "  }\n"
                         + "}";
  String createRecord1OverriddenTTL = "{\n"
                                      + "  \"ttl\": 10000000,\n"
                                      + "  \"rdata\": {\n"
                                      + "    \"address\": \"192.0.2.1\"\n"
                                      + "  }\n"
                                      + "}";
  String createRecord2OverriddenTTL = "{\n"
                                      + "  \"ttl\": 10000000,\n"
                                      + "  \"rdata\": {\n"
                                      + "    \"address\": \"198.51.100.1\"\n"
                                      + "  }\n"
                                      + "}";
  String
      recordIds1And2 =
      "{\"status\": \"success\", \"data\": [\"/REST/ARecord/denominator.io/www.denominator.io/1\", \"/REST/ARecord/denominator.io/www.denominator.io/2\"], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  String
      records1And2 =
      "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"192.0.2.1\"}, \"record_id\": 1}, {\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"198.51.100.1\"}, \"record_id\": 2}], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

  public DynECTResourceRecordSetApiMockTest() throws IOException {
    records = Util.slurp(new InputStreamReader(getClass().getResourceAsStream("/records.json")));
    recordsByName = Util.slurp(
        new InputStreamReader(getClass().getResourceAsStream("/recordsByName.json")));
  }
}
