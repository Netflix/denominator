package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import denominator.Credentials;
import denominator.dynect.InvalidatableTokenProvider.Session;
import feign.Feign;
import feign.RetryableException;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.assertj.core.api.Fail.failBecauseExceptionWasNotThrown;
import static org.assertj.core.groups.Tuple.tuple;

public class DynECTTest {

  @Rule
  public MockDynECTServer server = new MockDynECTServer();

  @Test
  public void hasAllGeoPermissions() throws Exception {
    server.enqueue(new MockResponse().setBody(allGeoPermissions));

    assertThat(mockApi().hasAllGeoPermissions().data).isTrue();

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport")
        .hasBody(
            "{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}");
  }

  @Test
  public void doesntHaveGeoPermissions() throws Exception {
    server.enqueue(new MockResponse().setBody(noGeoPermissions));

    assertThat(mockApi().hasAllGeoPermissions().data).isFalse();

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport")
        .hasBody(
            "{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}");
  }

  @Test
  public void zonesWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(zones));

    assertThat(mockApi().zones().data)
        .containsExactly("denominator.io");

    server.assertRequest().hasMethod("GET").hasPath("/Zone");
  }

  @Test
  public void populatesServiceClass() throws Exception {
    server.enqueue(new MockResponse().setBody(serviceNS));

    assertThat(
        mockApi().recordsInZoneByNameAndType("denominator.io.", "denominator.io.", "NS").data)
        .extracting("serviceClass")
        .containsExactly("Primary");

    server.assertRequest().hasMethod("GET")
        .hasPath("/NSRecord/denominator.io./denominator.io.?detail=Y");
  }

  @Test
  public void incompleteRetriesOn200() throws Exception {
    server.enqueue(new MockResponse().setBody(incomplete));
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
    server.enqueueSessionResponse(); // mismatch invalidates session!
    server.enqueue(new MockResponse().setBody(zones));

    mockApi().zones();

    server.assertRequest().hasMethod("GET").hasPath("/Zone");
    server.assertSessionRequest();
    server.assertRequest().hasMethod("GET").hasPath("/Zone");
  }

  /**
   * Eventhough there's no task api, unless we retry zone operations, users who attempt to delete
   * zones will almost certainly fail.
   */
  @Test
  public void blockedSlowlyRetries() throws Exception {
    long start = System.currentTimeMillis();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(taskBlocking));

    try {
      mockApi().zones();
      failBecauseExceptionWasNotThrown(RetryableException.class);
    } catch (RetryableException re) {
      assertThat(System.currentTimeMillis() - start)
          .isBetween(4000l, 5100l); // roughly 1 second per try, max 5 seconds.
      DynECTException e = (DynECTException) re.getCause();
      assertThat(e)
          .hasMessage(
              "status failure: [ILLEGAL_OPERATION: zone: Operation blocked by current task, task_name: ProvisionZone, task_id: 39120953]");
      assertThat(e.status()).isEqualTo("failure");
      assertThat(e.messages()).extracting("code", "info")
          .containsOnly(
              tuple("ILLEGAL_OPERATION", "zone: Operation blocked by current task"),
              tuple(null, "task_name: ProvisionZone"),
              tuple(null, "task_id: 39120953")
          );
    }
  }

  @Test
  public void alreadyExistsDoesntRetry() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(400).setBody(targetExists));

    try {
      mockApi().zones();
      failBecauseExceptionWasNotThrown(DynECTException.class);
    } catch (DynECTException e) {
      assertThat(e)
          .hasMessage(
              "status failure: [TARGET_EXISTS: name: Name already exists, create: You already have this zone.]");
      assertThat(e.status()).isEqualTo("failure");
      assertThat(e.messages()).extracting("code", "info")
          .containsOnly(
              tuple("TARGET_EXISTS", "name: Name already exists"),
              tuple(null, "create: You already have this zone.")
          );
    }
  }

  DynECT mockApi() {
    DynECTProvider.FeignModule module = new DynECTProvider.FeignModule();
    DynECTProvider provider = new DynECTProvider() {
      @Override
      public String url() {
        return server.url();
      }
    };
    javax.inject.Provider<Credentials> credentials = new javax.inject.Provider<Credentials>() {

      @Override
      public Credentials get() {
        return server.credentials();
      }

    };
    AtomicReference<Boolean> sessionValid = module.sessionValid();
    DynECTErrorDecoder errorDecoder = new DynECTErrorDecoder(sessionValid);
    Feign feign = module.feign(module.logger(), module.logLevel(), errorDecoder);
    Session sessionApi = feign.newInstance(new SessionTarget(provider));
    InvalidatableTokenProvider
        tokenProvider =
        new InvalidatableTokenProvider(provider, sessionApi, credentials, sessionValid);

    // hard-coding session to be true to avoid further boilerplate.
    tokenProvider.lastCredentialsHashCode = credentials.get().hashCode();
    tokenProvider.value = "foo";
    sessionValid.set(true);

    return feign.newInstance(new DynECTTarget(new DynECTProvider() {
      @Override
      public String url() {
        return server.url();
      }
    }, tokenProvider));
  }

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
  static String serviceNS = "{\n"
                            + "  \"status\": \"success\",\n"
                            + "  \"data\": [\n"
                            + "    {\n"
                            + "      \"zone\": \"denominator.io\",\n"
                            + "      \"service_class\": \"Primary\",\n"
                            + "      \"ttl\": 86400,\n"
                            + "      \"fqdn\": \"denominator.io\",\n"
                            + "      \"record_type\": \"NS\",\n"
                            + "      \"rdata\": {\n"
                            + "        \"nsdname\": \"ns1.p21.dynect.net.\"\n"
                            + "      },\n"
                            + "      \"record_id\": 156826633\n"
                            + "    }\n"
                            + "  ],\n"
                            + "  \"job_id\": 1566905457,\n"
                            + "  \"msgs\": [\n"
                            + "    {\n"
                            + "      \"INFO\": \"detail: Found 1 records\",\n"
                            + "      \"SOURCE\": \"BLL\",\n"
                            + "      \"ERR_CD\": null,\n"
                            + "      \"LVL\": \"INFO\"\n"
                            + "    }\n"
                            + "  ]\n"
                            + "}";
  static String
      zones =
      "{\"status\": \"success\", \"data\": [\"/REST/Zone/denominator.io/\"], \"job_id\": 260657587, \"msgs\": [{\"INFO\": \"get: Your 1 zone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
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
}
