package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.DNSApiManager;

import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatus;
import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatusResponse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetworkStatusReadableMockTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();

  @Test
  public void singleRequestOnSuccess() throws Exception {
    server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());

    server.assertSoapBody(getNeustarNetworkStatus);
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertSoapBody(getNeustarNetworkStatus);
  }
}
