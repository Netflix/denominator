package denominator.ultradns;

import static java.lang.String.format;

import java.io.IOException;
import java.lang.reflect.Type;

import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import feign.codec.SAXDecoder;

class UltraDNSErrorDecoder extends SAXDecoder implements ErrorDecoder {

    @Override
    public Object decode(String methodKey, Response response, Type type) throws Throwable {
        try {
            // in case of error parsing, we can access the original contents.
            response = bufferResponse(response);
            UltraDNSError error = UltraDNSError.class.cast(super.decode(methodKey, response, type));
            if (error == null)
                throw FeignException.errorStatus(methodKey, response);
            String message = format("%s failed", methodKey);
            if (error.code != -1)
                message = format("%s with error %s", message, error.code);
            if (error.description != null)
                message = format("%s: %s", message, error.description);
            switch (error.code) {
            case UltraDNSException.SYSTEM_ERROR:
                throw new RetryableException(message, null);
            case UltraDNSException.UNKNOWN:
                if (message.indexOf("Cannot find") == -1)
                    break;
            case UltraDNSException.ZONE_NOT_FOUND:
            case UltraDNSException.RESOURCE_RECORD_NOT_FOUND:
            case UltraDNSException.ACCOUNT_NOT_FOUND:
            case UltraDNSException.POOL_NOT_FOUND:
            case UltraDNSException.DIRECTIONALPOOL_NOT_FOUND:
            case UltraDNSException.DIRECTIONALPOOL_RECORD_NOT_FOUND:
            case UltraDNSException.POOL_RECORD_NOT_FOUND:
            case UltraDNSException.GROUP_NOT_FOUND:
                // TODO: decide whether to fallback
                break;
            case UltraDNSException.ZONE_ALREADY_EXISTS:
            case UltraDNSException.RESOURCE_RECORD_ALREADY_EXISTS:
            case UltraDNSException.POOL_ALREADY_EXISTS:
            case UltraDNSException.POOL_RECORD_ALREADY_EXISTS:
                // TODO: decide whether to fallback
                break;
            }
            throw new UltraDNSException(message, error.code);
        } catch (IOException e) {
            throw FeignException.errorStatus(methodKey, response);
        }
    }

    @Override
    protected ContentHandlerWithResult typeToNewHandler(Type type) {
        return new UltraDNSError();
    }

    static class UltraDNSError extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {

        private StringBuilder currentText = new StringBuilder();
        private int code = -1;
        private String description;

        @Override
        public UltraDNSError getResult() {
            return (code == -1 && description == null) ? null : this;
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.endsWith("errorCode")) {
                code = Integer.parseInt(currentText.toString().trim());
            } else if (qName.endsWith("errorDescription") || qName.endsWith("faultstring")) {
                description = Strings.emptyToNull(currentText.toString().trim());
            }
            currentText = new StringBuilder();
        }

        @Override
        public void characters(char ch[], int start, int length) {
            currentText.append(ch, start, length);
        }
    }

    static Response bufferResponse(Response response) throws IOException {
        if (response.body() == null)
            return response;
        String body = CharStreams.toString(response.body().asReader());
        return Response.create(response.status(), response.reason(), response.headers(), body);
    }
}
