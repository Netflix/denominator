package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import dagger.ObjectGraph;
import denominator.model.Zone;
import feign.Feign;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DynECTTest {

  MockDynECTServer server;

  static final String
      noneWithName =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 478442824, \"msgs\": [{\"INFO\": \"node: Node is not in the zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}, {\"INFO\": \"get_tree: Node name not found within the zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static final String
      noneWithNameAndType =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 478442805, \"msgs\": [{\"INFO\": \"node: Not in zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}, {\"INFO\": \"detail: Host is not in this zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static String allGeoPermissions = "{\n"
                                    + "    \"status\": \"success\",\n"
                                    + "    \"data\": {\n"
                                    + "        \"forbidden\": [],\n"
                                    + "        \"admin_override\": \"1\",\n"
                                    + "        \"allowed\": [{\n"
                                    + "            \"reason\": [\"\"],\n"
                                    + "            \"name\": \"GeoUpdate\",\n"
                                    + "            \"zone\": []\n"
                                    + "        }, {\n"
                                    + "            \"reason\": [\"\"],\n"
                                    + "            \"name\": \"GeoGet\",\n"
                                    + "            \"zone\": []\n"
                                    + "        }, {\n"
                                    + "            \"reason\": [\"\"],\n"
                                    + "            \"name\": \"GeoDelete\",\n"
                                    + "            \"zone\": []\n"
                                    + "        }, {\n"
                                    + "            \"reason\": [\"\"],\n"
                                    + "            \"name\": \"GeoActivate\",\n"
                                    + "            \"zone\": []\n"
                                    + "        }, {\n"
                                    + "            \"reason\": [\"\"],\n"
                                    + "            \"name\": \"GeoDeactivate\",\n"
                                    + "            \"zone\": []\n"
                                    + "        }]\n"
                                    + "    },\n"
                                    + "    \"job_id\": 428974777\n"
                                    + "}";
  static String noGeoPermissions = "{\n"
                                   + "    \"status\": \"success\",\n"
                                   + "    \"data\": {\n"
                                   + "        \"allowed\": [],\n"
                                   + "        \"admin_override\": \"1\",\n"
                                   + "        \"forbidden\": [{\n"
                                   + "            \"reason\": [\"permission not found\"],\n"
                                   + "            \"name\": \"GeoUpdate\",\n"
                                   + "            \"zone\": []\n"
                                   + "        }, {\n"
                                   + "            \"reason\": [\"permission not found\"],\n"
                                   + "            \"name\": \"GeoGet\",\n"
                                   + "            \"zone\": []\n"
                                   + "        }, {\n"
                                   + "            \"reason\": [\"permission not found\"],\n"
                                   + "            \"name\": \"GeoDelete\",\n"
                                   + "            \"zone\": []\n"
                                   + "        }, {\n"
                                   + "            \"reason\": [\"permission not found\"],\n"
                                   + "            \"name\": \"GeoActivate\",\n"
                                   + "            \"zone\": []\n"
                                   + "        }, {\n"
                                   + "            \"reason\": [\"permission not found\"],\n"
                                   + "            \"name\": \"GeoDeactivate\",\n"
                                   + "            \"zone\": []\n"
                                   + "        }]\n"
                                   + "    },\n"
                                   + "    \"job_id\": 428974777\n"
                                   + "}";
  static String
      zones =
      "{\"status\": \"success\", \"data\": [\"/REST/Zone/0.0.0.0.d.6.e.0.0.a.2.ip6.arpa/\", \"/REST/Zone/126.12.44.in-addr.arpa/\", \"/REST/Zone/denominator.io/\"], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 3 zones\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static String
      noZones =
      "{\"status\": \"success\", \"data\": [], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 0 zones\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static String incomplete = "{\"status\": \"incomplete\", \"data\": null, \"job_id\": 399831496}";
  static String
      running =
      "{\"status\": \"running\", \"data\": {}, \"job_id\": 274509427, \"msgs\": [{\"INFO\": \"token: This session already has a job running\", \"SOURCE\": \"API-B\", \"ERR_CD\": \"OPERATION_FAILED\", \"LVL\": \"ERROR\"}]}";
  static String
      mismatch =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 305900967, \"msgs\": [{\"INFO\": \"login: IP address does not match current session\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"INVALID_DATA\", \"LVL\": \"ERROR\"}, {\"INFO\": \"login: There was a problem with your credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static String
      taskBlocking =
      "[{\"status\": \"failure\", \"data\": {}, \"job_id\": 275545493, \"msgs\": [{\"INFO\": \"zone: Operation blocked by current task\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"ILLEGAL_OPERATION\", \"LVL\": \"ERROR\"}, {\"INFO\": \"task_name: ProvisionZone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}, {\"INFO\": \"task_id: 39120953\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}]";
  static String
      targetExists =
      "[{\"status\": \"failure\", \"data\": {}, \"job_id\": 275533917, \"msgs\": [{\"INFO\": \"name: Name already exists\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"TARGET_EXISTS\", \"LVL\": \"ERROR\"}, {\"INFO\": \"create: You already have this zone.\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}]";

  @Test
  public void hasAllGeoPermissions() throws Exception {
    server.enqueue(new MockResponse().setBody(allGeoPermissions));

    assertTrue(mockApi().hasAllGeoPermissions().data);

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport")
        .hasBody(
            "{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}");
  }

  @Test
  public void doesntHaveGeoPermissions() throws Exception {
    server.enqueue(new MockResponse().setBody(noGeoPermissions));

    assertFalse(mockApi().hasAllGeoPermissions().data);

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport")
        .hasBody(
            "{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}");
  }

  @Test
  public void zonesWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(zones));

    Iterator<Zone> iterator = mockApi().zones().data.iterator();
    iterator.next();
    iterator.next();
    assertEquals(iterator.next(), Zone.create("denominator.io"));

    server.assertRequest().hasMethod("GET").hasPath("/Zone");
  }

  @Test
  public void incompleteRetries() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(400).setBody(incomplete));
    server.enqueue(new MockResponse().setBody(zones));

    mockApi().zones();

    server.assertRequest().hasMethod("GET").hasPath("/Zone");
    server.assertRequest().hasMethod("GET").hasPath("/Zone");
  }

  @Test
  public void runningRetries() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(400).setBody(running));
    server.enqueue(new MockResponse().setBody(zones));

    mockApi().zones();

    server.assertRequest().hasMethod("GET").hasPath("/Zone");
    server.assertRequest().hasMethod("GET").hasPath("/Zone");
  }

  @Test
  public void ipMisMatchRetries() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(400).setBody(mismatch));
    server.enqueue(new MockResponse().setBody(zones));

    mockApi().zones();

    server.assertRequest().hasMethod("GET").hasPath("/Zone");
    server.assertRequest().hasMethod("GET").hasPath("/Zone");
  }

  @Test
  public void blockedDoesntRetry() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));

    try {
      mockApi().zones();
      fail();
    } catch (DynECTException e) {
      assertEquals(e.getMessage(),
                   "status failure: [ILLEGAL_OPERATION: zone: Operation blocked by current task, task_name: ProvisionZone, task_id: 39120953]");
      assertEquals(e.status(), "failure");
      assertEquals(e.messages().get(0).code(), "ILLEGAL_OPERATION");
      assertEquals(e.messages().get(0).info(), "zone: Operation blocked by current task");
      assertFalse(e.messages().get(1).code() != null);
      assertEquals(e.messages().get(1).info(), "task_name: ProvisionZone");
      assertFalse(e.messages().get(1).code() != null);
      assertEquals(e.messages().get(2).info(), "task_id: 39120953");
    }
  }

  @Test
  public void alreadyExistsDoesntRetry() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(400).setBody(targetExists));

    try {
      mockApi().zones();
      fail();
    } catch (DynECTException e) {
      assertEquals(e.getMessage(),
                   "status failure: [TARGET_EXISTS: name: Name already exists, create: You already have this zone.]");
      assertEquals(e.status(), "failure");
      assertEquals(e.messages().size(), 2);
      assertEquals(e.messages().get(0).code(), "TARGET_EXISTS");
      assertEquals(e.messages().get(0).info(), "name: Name already exists");
      assertFalse(e.messages().get(1).code() != null);
      assertEquals(e.messages().get(1).info(), "create: You already have this zone.");
    }
  }

  DynECT mockApi() {
    return ObjectGraph.create(new DynECTProvider.FeignModule()).get(Feign.class)
        .newInstance(new DynECTTarget(new DynECTProvider() {
          @Override
          public String url() {
            return server.url();
          }
        }, new javax.inject.Provider<String>() {

          @Override
          public String get() {
            return server.token();
          }

        }));
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDynECTServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
