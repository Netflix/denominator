package denominator.route53;

import static feign.codec.Decoders.eachFirstGroup;
import static java.lang.String.format;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xml.sax.helpers.DefaultHandler;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.codec.SAXDecoder;

class Route53ErrorDecoder extends SAXDecoder<Route53ErrorDecoder.Route53Error> implements ErrorDecoder {

    @Inject
    Route53ErrorDecoder(Provider<Route53ErrorDecoder.Route53Error> handlers) {
        super(handlers);
    }

    // visible for testing;
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if ("Route53#changeBatch(String,List)".equals(methodKey)) {
                List<String> messages = response.body() != null ? messagesDecoder.decode(response.body().asReader(),
                        null) : null;
                return new InvalidChangeBatchException(methodKey, messages);
            }
            Route53Error error = response.body() != null ? super.decode(response.body().asReader(), null) : null;
            if (error == null)
                return FeignException.errorStatus(methodKey, response);
            String message = format("%s failed with error %s", methodKey, error.code);
            if (error.message != null)
                message = format("%s: %s", message, error.message);
            if ("RequestExpired".equals(error.code) || "InternalError".equals(error.code)
                    || "InternalFailure".equals(error.code)) {
                return new RetryableException(message, null);
            } else if ("Throttling".equals(error.code) || "PriorRequestNotComplete".equals(error.code)) {
                // backoff at least a second.
                return new RetryableException(message, new Date(currentTimeMillis() + 1000));
            } else if (error.code.startsWith("NoSuch")) {
                // consider not found exception
            }
            return new Route53Exception(message, error.code);
        } catch (IOException e) {
            return FeignException.errorStatus(methodKey, response);
        }
    }

    Decoder.TextStream<List<String>> messagesDecoder = eachFirstGroup("<Message>([^<]+)</Message>");

    static class Route53Error extends DefaultHandler implements
            feign.codec.SAXDecoder.ContentHandlerWithResult<Route53Error> {
        @Inject
        Route53Error() {
        }

        private StringBuilder currentText = new StringBuilder();
        private String code;
        private String message;

        @Override
        public Route53Error result() {
            return this;
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.equals("Code")) {
                code = currentText.toString().trim();
            } else if (qName.equals("Message")) {
                message = currentText.toString().trim();
            }
            currentText = new StringBuilder();
        }

        @Override
        public void characters(char ch[], int start, int length) {
            currentText.append(ch, start, length);
        }
    }
}
