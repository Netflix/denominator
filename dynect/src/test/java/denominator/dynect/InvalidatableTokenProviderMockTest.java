package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.DNSApiManager;

import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.sessionValid;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvalidatableTokenProviderMockTest {

  @Rule
  public MockDynECTServer server = new MockDynECTServer();

  @Test
  public void successThenFailure() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(sessionValid));
    server.enqueue(new MockResponse().setBody(sessionValid));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(
        "{\"status\": \"failure\", \"data\": {}, \"job_id\": 427275274, \"msgs\": [{\"INFO\": \"login: Bad or expired credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"INVALID_DATA\", \"LVL\": \"ERROR\"}, {\"INFO\": \"login: There was a problem with your credentials\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}"));

    DNSApiManager api = server.connect();

    assertTrue(api.checkConnection());
    assertTrue(api.checkConnection());
    assertFalse(api.checkConnection());

    server.assertSessionRequest();
    server.assertRequest().hasMethod("GET").hasPath("/Session");
    server.assertRequest().hasMethod("GET").hasPath("/Session");
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401));

    assertFalse(server.connect().checkConnection());

    server.assertSessionRequest();
  }
}
