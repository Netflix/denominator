package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

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

  static final String
      noneWithName =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 478442824, \"msgs\": [{\"INFO\": \"node: Node is not in the zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}, {\"INFO\": \"get_tree: Node name not found within the zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static final String
      noneWithNameAndType =
      "{\"status\": \"failure\", \"data\": {}, \"job_id\": 478442805, \"msgs\": [{\"INFO\": \"node: Not in zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"NOT_FOUND\", \"LVL\": \"ERROR\"}, {\"INFO\": \"detail: Host is not in this zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
  static String allGeoPermissions = ""//
                                    + "{\n"//
                                    + "    \"status\": \"success\",\n"//
                                    + "    \"data\": {\n"//
                                    + "        \"forbidden\": [],\n"//
                                    + "        \"admin_override\": \"1\",\n"//
                                    + "        \"allowed\": [{\n"//
                                    + "            \"reason\": [\"\"],\n"//
                                    + "            \"name\": \"GeoUpdate\",\n"//
                                    + "            \"zone\": []\n"//
                                    + "        }, {\n"//
                                    + "            \"reason\": [\"\"],\n"//
                                    + "            \"name\": \"GeoGet\",\n"//
                                    + "            \"zone\": []\n"//
                                    + "        }, {\n"//
                                    + "            \"reason\": [\"\"],\n"//
                                    + "            \"name\": \"GeoDelete\",\n"//
                                    + "            \"zone\": []\n"//
                                    + "        }, {\n"//
                                    + "            \"reason\": [\"\"],\n"//
                                    + "            \"name\": \"GeoActivate\",\n"//
                                    + "            \"zone\": []\n"//
                                    + "        }, {\n"//
                                    + "            \"reason\": [\"\"],\n"//
                                    + "            \"name\": \"GeoDeactivate\",\n"//
                                    + "            \"zone\": []\n"//
                                    + "        }]\n"//
                                    + "    },\n"//
                                    + "    \"job_id\": 428974777\n"//
                                    + "}";
  static String noGeoPermissions = ""//
                                   + "{\n"//
                                   + "    \"status\": \"success\",\n"//
                                   + "    \"data\": {\n"//
                                   + "        \"allowed\": [],\n"//
                                   + "        \"admin_override\": \"1\",\n"//
                                   + "        \"forbidden\": [{\n"//
                                   + "            \"reason\": [\"permission not found\"],\n"//
                                   + "            \"name\": \"GeoUpdate\",\n"//
                                   + "            \"zone\": []\n"//
                                   + "        }, {\n"//
                                   + "            \"reason\": [\"permission not found\"],\n"//
                                   + "            \"name\": \"GeoGet\",\n"//
                                   + "            \"zone\": []\n"//
                                   + "        }, {\n"//
                                   + "            \"reason\": [\"permission not found\"],\n"//
                                   + "            \"name\": \"GeoDelete\",\n"//
                                   + "            \"zone\": []\n"//
                                   + "        }, {\n"//
                                   + "            \"reason\": [\"permission not found\"],\n"//
                                   + "            \"name\": \"GeoActivate\",\n"//
                                   + "            \"zone\": []\n"//
                                   + "        }, {\n"//
                                   + "            \"reason\": [\"permission not found\"],\n"//
                                   + "            \"name\": \"GeoDeactivate\",\n"//
                                   + "            \"zone\": []\n"//
                                   + "        }]\n"//
                                   + "    },\n"//
                                   + "    \"job_id\": 428974777\n"//
                                   + "}";
  static String
      zones =
      "{\"status\": \"success\", \"data\": [\"/REST/Zone/0.0.0.0.d.6.e.0.0.a.2.ip6.arpa/\", \"/REST/Zone/126.12.44.in-addr.arpa/\", \"/REST/Zone/denominator.io/\"], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 3 zones\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
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
  static javax.inject.Provider<String> lazyToken = new javax.inject.Provider<String>() {

    @Override
    public String get() {
      return "FFFFFFFFFF";
    }

  };

  static DynECT mockApi(final int port) {
    return ObjectGraph.create(new DynECTProvider.FeignModule()).get(Feign.class)
        .newInstance(new DynECTTarget(new DynECTProvider() {
          @Override
          public String url() {
            return "http://localhost:" + port;
          }
        }, lazyToken));
  }

  @Test
  public void hasAllGeoPermissions() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.play();

    try {
      DynECT api = mockApi(server.getPort());
      assertTrue(api.hasAllGeoPermissions().data);

      assertEquals(server.getRequestCount(), 1);
      RecordedRequest checkGeoPermissions = server.takeRequest();
      assertEquals(checkGeoPermissions.getRequestLine(), "POST /CheckPermissionReport HTTP/1.1");
      assertEquals(new String(checkGeoPermissions.getBody()),
                   "{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void doesntHaveGeoPermissions() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(noGeoPermissions));
    server.play();

    try {
      DynECT api = mockApi(server.getPort());
      assertFalse(api.hasAllGeoPermissions().data);

      assertEquals(server.getRequestCount(), 1);
      RecordedRequest checkGeoPermissions = server.takeRequest();
      assertEquals(checkGeoPermissions.getRequestLine(), "POST /CheckPermissionReport HTTP/1.1");
      assertEquals(new String(checkGeoPermissions.getBody()),
                   "{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void zonesWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody(zones));
    server.play();

    try {
      DynECT api = mockApi(server.getPort());
      Iterator<Zone> iterator = api.zones().data.iterator();
      iterator.next();
      iterator.next();
      assertEquals(iterator.next(), Zone.create("denominator.io"));

      assertEquals(server.getRequestCount(), 1);
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void incompleteRetries() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(incomplete));
    server.enqueue(new MockResponse().setBody(zones));
    server.play();

    DynECT api = mockApi(server.getPort());

    try {
      api.zones();

      assertEquals(server.getRequestCount(), 2);
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void runningRetries() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(running));
    server.enqueue(new MockResponse().setBody(zones));

    server.play();

    DynECT api = mockApi(server.getPort());

    try {
      api.zones();

      assertEquals(server.getRequestCount(), 2);
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void ipMisMatchRetries() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(mismatch));
    server.enqueue(new MockResponse().setBody(zones));

    server.play();

    DynECT api = mockApi(server.getPort());

    try {
      api.zones();

      assertEquals(server.getRequestCount(), 2);
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
      assertEquals(server.takeRequest().getRequestLine(), "GET /Zone HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void blockedDoesntRetry() throws IOException, InterruptedException {
    System.out.println(taskBlocking);

    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));
    server.play();

    DynECT api = mockApi(server.getPort());

    try {
      api.zones();
      fail();
    } catch (DynECTException e) {
      assertEquals(server.getRequestCount(), 1);
      assertEquals(e.getMessage(),
                   "status failure: [ILLEGAL_OPERATION: zone: Operation blocked by current task, task_name: ProvisionZone, task_id: 39120953]");

      assertEquals(e.status(), "failure");
      assertEquals(e.messages().size(), 3);
      assertEquals(e.messages().get(0).code(), "ILLEGAL_OPERATION");
      assertEquals(e.messages().get(0).info(), "zone: Operation blocked by current task");
      assertFalse(e.messages().get(1).code() != null);
      assertEquals(e.messages().get(1).info(), "task_name: ProvisionZone");
      assertFalse(e.messages().get(1).code() != null);
      assertEquals(e.messages().get(2).info(), "task_id: 39120953");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void alreadyExistsDoesntRetry() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    System.out.println(targetExists);
    server.enqueue(new MockResponse().setResponseCode(400).setBody(targetExists));
    server.play();

    DynECT api = mockApi(server.getPort());

    try {
      api.zones();
      fail();
    } catch (DynECTException e) {
      assertEquals(server.getRequestCount(), 1);
      assertEquals(e.getMessage(),
                   "status failure: [TARGET_EXISTS: name: Name already exists, create: You already have this zone.]");

      assertEquals(e.status(), "failure");
      assertEquals(e.messages().size(), 2);
      assertEquals(e.messages().get(0).code(), "TARGET_EXISTS");
      assertEquals(e.messages().get(0).info(), "name: Name already exists");
      assertFalse(e.messages().get(1).code() != null);
      assertEquals(e.messages().get(1).info(), "create: You already have this zone.");
    } finally {
      server.shutdown();
    }
  }
}
