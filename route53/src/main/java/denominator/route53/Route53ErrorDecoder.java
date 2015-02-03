package denominator.route53;

import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

import static feign.Util.resolveLastTypeParameter;
import static java.lang.String.format;

class Route53ErrorDecoder implements ErrorDecoder {

  private static final Type
      LIST_STRING =
      resolveLastTypeParameter(Messages.class, ContentHandlerWithResult.class);
  private final Decoder decoder;

  Route53ErrorDecoder(Decoder decoder) {
    this.decoder = decoder;
  }

  // visible for testing;
  protected long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public Exception decode(String methodKey, Response response) {
    try {
      if ("Route53#changeResourceRecordSets(String,List)".equals(methodKey)) {
        @SuppressWarnings("unchecked")
        List<String> messages = List.class.cast(decoder.decode(response, LIST_STRING));
        return new InvalidChangeBatchException(methodKey, messages);
      }
      Route53Error error = Route53Error.class.cast(decoder.decode(response, Route53Error.class));
      if (error == null) {
        return FeignException.errorStatus(methodKey, response);
      }
      String message = format("%s failed with error %s", methodKey, error.code);
      if (error.message != null) {
        message = format("%s: %s", message, error.message);
      }
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

  static class Messages extends DefaultHandler implements ContentHandlerWithResult<List<String>> {

    private final StringBuilder currentText = new StringBuilder();
    private final List<String> messages = new ArrayList<String>(10);

    @Override
    public List<String> result() {
      return messages;
    }

    @Override
    public void endElement(String uri, String name, String qName) {
      if (qName.equals("Message")) {
        messages.add(currentText.toString().trim());
      }
      currentText.setLength(0);
    }

    @Override
    public void characters(char ch[], int start, int length) {
      currentText.append(ch, start, length);
    }
  }

  static class Route53Error extends DefaultHandler
      implements ContentHandlerWithResult<Route53Error> {

    private final StringBuilder currentText = new StringBuilder();
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
      currentText.setLength(0);
    }

    @Override
    public void characters(char ch[], int start, int length) {
      currentText.append(ch, start, length);
    }
  }
}
