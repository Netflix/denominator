package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class LimitsReadableMockTest {

  MockDesignateServer server;

  String limitsResponse = "{\n"
                          + "  \"limits\": {\n"
                          + "    \"absolute\": {\n"
                          + "      \"maxDomains\": 20,\n"
                          + "      \"maxDomainRecords\": 5000\n"
                          + "    }\n"
                          + "  }\n"
                          + "}";

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
    server.assertRequest().hasPath("/v1/limits");
    server.assertRequest().hasPath("/v1/limits");
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
    server = new MockDesignateServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
