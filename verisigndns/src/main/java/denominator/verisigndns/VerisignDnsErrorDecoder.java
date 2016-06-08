package denominator.verisigndns;

import java.io.IOException;

import javax.inject.Inject;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

final class VerisignDnsErrorDecoder implements ErrorDecoder {

  private final Decoder decoder;

  VerisignDnsErrorDecoder(Decoder decoder) {
    this.decoder = decoder;
  }

  @Override
  public Exception decode(String methodKey, Response response) {

    try {
      Object errorObject = decoder.decode(response, VerisignDnsError.class);
      VerisignDnsError error = VerisignDnsError.class.cast(errorObject);
      if (error == null) {
        return FeignException.errorStatus(methodKey, response);
      }

      StringBuilder message = new StringBuilder();
      message.append(methodKey).append(" failed");
      if (error.code != null) {
        message.append(" with error ").append(error.code);
      }
      if (error.description != null) {
        message.append(": ").append(error.description);
      }

      int errorCode = -1;
      if (error.description.equalsIgnoreCase("The domain name could not be found.")) {
        errorCode = VerisignDnsException.DOMAIN_NOT_FOUND;
      } else if (error.description
          .equalsIgnoreCase("Domain already exists. Please verify your domain name.")) {
        errorCode = VerisignDnsException.DOMAIN_ALREADY_EXISTS;
      }

      return new VerisignDnsException(message.toString(), errorCode, error.description);
    } catch (IOException ignored) {
      return FeignException.errorStatus(methodKey, response);
    } catch (RuntimeException propagate) {
      return propagate;
    }
  }

  static class VerisignDnsError extends DefaultHandler implements
      ContentHandlerWithResult<VerisignDnsError> {

    private String description;
    private String code;

    @Inject
    VerisignDnsError() {
    }

    @Override
    public VerisignDnsError result() {
      return (code == null && description == null) ? null : this;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {

      if ("ns3:reason".equals(qName)) {
        description = attributes.getValue("description");
        code = attributes.getValue("code");
      }
    }
  }
}
