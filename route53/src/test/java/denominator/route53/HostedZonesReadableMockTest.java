package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.DNSApiManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostedZonesReadableMockTest {

  @Rule
  public MockRoute53Server server = new MockRoute53Server();

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
}
