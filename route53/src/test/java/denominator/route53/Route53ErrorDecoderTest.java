package denominator.route53;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Collections;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import static feign.Util.UTF_8;

public class Route53ErrorDecoderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  ErrorDecoder errors = new Route53ErrorDecoder(new Route53Provider.FeignModule().decoder());

  @Test
  public void requestExpired() throws Exception {
    thrown.expect(RetryableException.class);
    thrown.expectMessage("Route53.zones() failed with error RequestExpired: Request has expired. Timestamp date is 2013-06-07T12:16:22Z");

    Response
        response =
        response("<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
                 + "  <Errors>\n"
                 + "    <Error>\n"
                 + "      <Code>RequestExpired</Code>\n"
                 + "      <Message>Request has expired. Timestamp date is 2013-06-07T12:16:22Z</Message>\n"
                 + "    </Error>\n"
                 + "  </Errors>\n"
                 + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"
                 + "</Response>");
    throw errors.decode("Route53.zones()", response);
  }

  @Test
  public void internalError() throws Exception {
    thrown.expect(RetryableException.class);
    thrown.expectMessage("Route53.zones() failed with error InternalError");

    Response
        response =
        response("<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
                 + "  <Errors>\n"
                 + "    <Error>\n"
                 + "      <Code>InternalError</Code>\n"
                 + "    </Error>\n"
                 + "  </Errors>\n"
                 + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"
                 + "</Response>");
    throw errors.decode("Route53.zones()", response);
  }

  @Test
  public void internalFailure() throws Exception {
    thrown.expect(RetryableException.class);
    thrown.expectMessage("Route53.zones() failed with error InternalFailure");

    Response
        response =
        response("<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
                 + "  <Errors>\n"
                 + "    <Error>\n"
                 + "      <Type>Receiver</Type>\n"
                 + "      <Code>InternalFailure</Code>\n"
                 + "    </Error>\n"
                 + "  </Errors>\n"
                 + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"
                 + "</Response>");
    throw errors.decode("Route53.zones()", response);
  }

  @Test
  public void throttling() throws Exception {
    thrown.expect(RetryableException.class);
    thrown.expectMessage("Route53.zones() failed with error Throttling: Rate exceeded");

    Response
        response =
        response("<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
                 + "  <Errors>\n"
                 + "    <Error>\n"
                 + "      <Code>Throttling</Code>\n"
                 + "      <Message>Rate exceeded</Message>\n"
                 + "    </Error>\n"
                 + "  </Errors>\n"
                 + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"
                 + "</Response>");
    throw errors.decode("Route53.zones()", response);
  }

  @Test
  public void priorRequestNotComplete() throws Exception {
    thrown.expect(RetryableException.class);
    thrown.expectMessage("Route53.zones() failed with error PriorRequestNotComplete: The request was rejected because Route 53 was still processing a prior request.");

    Response
        response =
        response("<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
                 + "  <Errors>\n"
                 + "    <Error>\n"
                 + "      <Code>PriorRequestNotComplete</Code>\n"
                 + "      <Message>The request was rejected because Route 53 was still processing a prior request.</Message>\n"
                 + "    </Error>\n"
                 + "  </Errors>\n"
                 + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"
                 + "</Response>");
    throw errors.decode("Route53.zones()", response);
  }

  static Response response(String xml) {
    return Response
        .create(500, "ServerError", Collections.<String, Collection<String>>emptyMap(), xml, UTF_8);
  }
}
