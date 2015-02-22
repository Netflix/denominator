package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static denominator.clouddns.RackspaceApisTest.limitsResponse;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class LimitsReadableMockTest {

  MockCloudDNSServer server;

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

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockCloudDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
