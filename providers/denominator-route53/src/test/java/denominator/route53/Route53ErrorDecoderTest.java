package denominator.route53;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.reflect.TypeToken;

import denominator.route53.Route53.ZoneList;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

@Test(singleThreaded = true)
public class Route53ErrorDecoderTest {

    ErrorDecoder errorDecoder = new Route53ErrorDecoder();

    @Test(expectedExceptions = RetryableException.class, expectedExceptionsMessageRegExp = "Route53.zones\\(\\) failed with error RequestExpired: Request has expired. Timestamp date is 2013-06-07T12:16:22Z")
    public void requestExpired() throws Throwable {
        Response response = responseWithContent(""//
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"//
                + "<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"//
                + "  <Errors>\n"//
                + "    <Error>\n"//
                + "      <Code>RequestExpired</Code>\n"//
                + "      <Message>Request has expired. Timestamp date is 2013-06-07T12:16:22Z</Message>\n"//
                + "    </Error>\n"//
                + "  </Errors>\n"//
                + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"//
                + "</Response>");
        errorDecoder.decode("Route53.zones()", response, TypeToken.of(ZoneList.class));
    }

    @Test(expectedExceptions = RetryableException.class, expectedExceptionsMessageRegExp = "Route53.zones\\(\\) failed with error InternalError")
    public void internalError() throws Throwable {
        Response response = responseWithContent(""//
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"//
                + "<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"//
                + "  <Errors>\n"//
                + "    <Error>\n"//
                + "      <Code>InternalError</Code>\n"//
                + "    </Error>\n"//
                + "  </Errors>\n"//
                + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"//
                + "</Response>");
        errorDecoder.decode("Route53.zones()", response, TypeToken.of(ZoneList.class));
    }

    @Test(expectedExceptions = RetryableException.class, expectedExceptionsMessageRegExp = "Route53.zones\\(\\) failed with error InternalFailure")
    public void internalFailure() throws Throwable {
        Response response = responseWithContent(""//
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"//
                + "<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"//
                + "  <Errors>\n"//
                + "    <Error>\n"//
                + "      <Type>Receiver</Type>\n"//
                + "      <Code>InternalFailure</Code>\n"//
                + "    </Error>\n"//
                + "  </Errors>\n"//
                + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"//
                + "</Response>");
        errorDecoder.decode("Route53.zones()", response, TypeToken.of(ZoneList.class));
    }

    @Test(expectedExceptions = RetryableException.class, expectedExceptionsMessageRegExp = "Route53.zones\\(\\) failed with error Throttling: Rate exceeded")
    public void throttling() throws Throwable {
        Response response = responseWithContent(""//
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"//
                + "<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"//
                + "  <Errors>\n"//
                + "    <Error>\n"//
                + "      <Code>Throttling</Code>\n"//
                + "      <Message>Rate exceeded</Message>\n"//
                + "    </Error>\n"//
                + "  </Errors>\n"//
                + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"//
                + "</Response>");
        errorDecoder.decode("Route53.zones()", response, TypeToken.of(ZoneList.class));
    }

    @Test(expectedExceptions = RetryableException.class, expectedExceptionsMessageRegExp = "Route53.zones\\(\\) failed with error PriorRequestNotComplete: The request was rejected because Route 53 was still processing a prior request.")
    public void priorRequestNotComplete() throws Throwable {
        Response response = responseWithContent(""//
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"//
                + "<Response xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"//
                + "  <Errors>\n"//
                + "    <Error>\n"//
                + "      <Code>PriorRequestNotComplete</Code>\n"//
                + "      <Message>The request was rejected because Route 53 was still processing a prior request.</Message>\n"//
                + "    </Error>\n"//
                + "  </Errors>\n"//
                + "  <RequestID>dc94a37b0-e297-4ab7-83c8-791a0fc8f613</RequestID>\n"//
                + "</Response>");
        errorDecoder.decode("Route53.zones()", response, TypeToken.of(ZoneList.class));
    }

    static Response responseWithContent(String content) {
        return Response.create(500, "ServerError", ImmutableListMultimap.<String, String> of(), content);
    }
}
