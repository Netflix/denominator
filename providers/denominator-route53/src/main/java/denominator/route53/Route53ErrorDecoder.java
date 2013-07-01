package denominator.route53;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Ticker;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import feign.codec.SAXDecoder;

class Route53ErrorDecoder extends SAXDecoder implements ErrorDecoder {

    private final Ticker ticker;

    public Route53ErrorDecoder(Ticker ticker) {
        this.ticker = ticker;
    }

    public Route53ErrorDecoder() {
        this(Ticker.systemTicker());
    }

    @Override
    public Object decode(String methodKey, Response response, Type type) throws Throwable {
        try {
            Route53Error error = Route53Error.class.cast(super.decode(methodKey, response, type));
            String message = format("%s failed with error %s", methodKey, error.code);
            if (error.message != null)
                message = format("%s: %s", message, error.message);
            if ("RequestExpired".equals(error.code) || "InternalError".equals(error.code)
                    || "InternalFailure".equals(error.code)) {
                throw new RetryableException(message, null);
            } else if ("Throttling".equals(error.code) || "PriorRequestNotComplete".equals(error.code)) {
                // backoff at least a second.
                throw new RetryableException(message, new Date(NANOSECONDS.toMillis(ticker.read()) + 1000));
            } else if (error.code.startsWith("NoSuch")) {
                // consider fallback
            }
            throw new Route53Exception(message, error.code);
        } catch (IOException e) {
            throw FeignException.errorStatus(methodKey, response);
        }
    }

    @Override
    protected ContentHandlerWithResult typeToNewHandler(Type type) {
        return new Route53Error();
    }

    static class Route53Error extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {

        private StringBuilder currentText = new StringBuilder();
        private String code;
        private String message;

        @Override
        public Route53Error getResult() {
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
