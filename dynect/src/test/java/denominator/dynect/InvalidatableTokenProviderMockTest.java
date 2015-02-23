package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.badSession;
import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.sessionValid;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class InvalidatableTokenProviderMockTest {

  MockDynECTServer server;

  @Test
  public void successThenFailure() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(sessionValid));
    server.enqueue(new MockResponse().setBody(sessionValid));
    server.enqueue(new MockResponse().setResponseCode(400).setBody(badSession));

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

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDynECTServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
