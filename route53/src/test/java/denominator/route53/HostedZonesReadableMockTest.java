package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.DNSApiManager;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class HostedZonesReadableMockTest {

  MockRoute53Server server;

  @Test
  public void singleRequestOnSuccess() throws Exception {
    server.enqueue(new MockResponse().setBody("<ListHostedZonesResponse />"));

    DNSApiManager api = server.connect();
    assertTrue(api.checkConnection());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone");
  }

  @Test
  public void singleRequestOnFailure() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(403).setBody(
        "<ErrorResponse xmlns=\"https:route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <Error>\n"
        + "    <Type>Sender</Type>\n"
        + "    <Code>InvalidClientTokenId</Code>\n"
        + "    <Message>The security token included in the request is invalid</Message>\n"
        + "  </Error>\n"
        + "  <RequestId>d3801bc8-f70d-11e2-8a6e-435ba83aa63f</RequestId>\n"
        + "</ErrorResponse>"));

    DNSApiManager api = server.connect();
    assertFalse(api.checkConnection());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone");
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockRoute53Server();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
