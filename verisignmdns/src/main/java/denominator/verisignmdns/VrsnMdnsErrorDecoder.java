/**
 * 
 */
package denominator.verisignmdns;

import java.io.IOException;

import static java.lang.String.format;

import javax.inject.Inject;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.sax.SAXDecoder.ContentHandlerWithResult;
import static feign.Util.UTF_8;
import static denominator.common.Util.slurp;

/**
 * @author smahurpawar
 *
 */
public class VrsnMdnsErrorDecoder implements ErrorDecoder {
	private static final String ERROR_RESPONSE_REASON_ELEMENT = ":reason";
	private static final String ERROR_RESPONSE_ATTRIBUTE_CODE = "code";
	private static final String ERROR_RESPONSE_ATTRIBUTE_DESCRIPTION = "description";
	
    private final Decoder decoder;

    @Inject
    VrsnMdnsErrorDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            // in case of error parsing, we can access the original contents.
            response = bufferResponse(response);
            VrsnMdnsError error = VrsnMdnsError.class.cast(decoder.decode(response, VrsnMdnsError.class));
            if (error == null)
                return FeignException.errorStatus(methodKey, response);
            String message = format("%s failed", methodKey);
            if (error.code != -1)
                message = format("%s with error %s", message, error.code);
            if (error.description != null)
                message = format("%s: %s", message, error.description);
           
            return new VrsnMdnsException(message, error.code);
        } catch (IOException ignored) {
            return FeignException.errorStatus(methodKey, response);
        } catch (Exception propagate) {
            return propagate;
        }
    }

    static class VrsnMdnsError extends DefaultHandler implements ContentHandlerWithResult<VrsnMdnsError> {
        @Inject
        VrsnMdnsError() {
        }

        private StringBuilder currentText = new StringBuilder();
        private int code = -1;
        private String description;

        @Override
        public VrsnMdnsError result() {
            return (code == -1 && description == null) ? null : this;
        }

        @Override
        public void startElement (String uri, String localName,
                String qName, Attributes attributes) throws SAXException {
        	if (qName.endsWith(ERROR_RESPONSE_REASON_ELEMENT)) {
        		try {
             	   Integer tempCode = 
             			VrsnConstants.MDNS_ERROR_TO_INT_CODE_MAP.get(attributes.getValue(ERROR_RESPONSE_ATTRIBUTE_CODE));
             	   code = tempCode.intValue();
             	   
                } catch (Exception ex) {
             	   //ignore
                }
        		description = attributes.getValue(ERROR_RESPONSE_ATTRIBUTE_DESCRIPTION);
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
        String body = slurp(response.body().asReader());
        return Response.create(response.status(), response.reason(), response.headers(), body, UTF_8);
    }

}
