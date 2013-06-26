package denominator.ultradns;

import static java.lang.String.format;

import java.io.IOException;

import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import feign.codec.SAXDecoder;

class UltraDNSErrorDecoder extends SAXDecoder implements ErrorDecoder {

    @Override
    public Object decode(String methodKey, Response response, TypeToken<?> type) {
        try {
            UltraDNSError error = UltraDNSError.class.cast(super.decode(methodKey, response, type));
            String message = format("%s failed with error %s", methodKey, error.code);
            if (error.description != null)
                message = format("%s: %s", message, error.description);
            switch (error.code) {
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
    protected ContentHandlerWithResult typeToNewHandler(TypeToken<?> type) {
        return new UltraDNSError();
    }

    static class UltraDNSError extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {

        private StringBuilder currentText = new StringBuilder();
        private int code = -1;
        private String description;

        @Override
        public UltraDNSError getResult() {
            return this;
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.endsWith("errorCode")) {
                code = Integer.parseInt(currentText.toString().trim());
            } else if (qName.endsWith("errorDescription")) {
                description = Strings.emptyToNull(currentText.toString().trim());
            }
            currentText = new StringBuilder();
        }

        @Override
        public void characters(char ch[], int start, int length) {
            currentText.append(ch, start, length);
        }
    }
}
