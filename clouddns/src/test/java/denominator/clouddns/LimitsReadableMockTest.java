package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.DNSApiManager;

import static denominator.clouddns.RackspaceApisTest.limitsResponse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LimitsReadableMockTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

  @Test
  public void implicitlyStartsSessionWhichIsReusedForLaterRequests() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));
    server.enqueue(new MockResponse().setBody(limitsResponse));
    server.enqueue(new MockResponse().setBody(limitsResponse));

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());
    assertTrue(api.checkConnection());

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/limits");
    server.assertRequest().hasPath("/v1.0/123123/limits");
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setResponseCode(401));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertAuthRequest();
  }
}
