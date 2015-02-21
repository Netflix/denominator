package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatus;
import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatusResponse;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class NetworkStatusReadableMockTest {

  MockUltraDNSServer server;

  @Test
  public void singleRequestOnSuccess() throws Exception {
    server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());

    server.assertRequestHasBody(getNeustarNetworkStatus);
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertRequestHasBody(getNeustarNetworkStatus);
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockUltraDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
