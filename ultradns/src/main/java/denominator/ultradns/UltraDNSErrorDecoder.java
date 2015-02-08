package denominator.ultradns;

import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

import javax.inject.Inject;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

import static denominator.common.Util.slurp;
import static feign.Util.UTF_8;
import static java.lang.String.format;

class UltraDNSErrorDecoder implements ErrorDecoder {

  private final Decoder decoder;

  UltraDNSErrorDecoder(Decoder decoder) {
    this.decoder = decoder;
  }

  static Response bufferResponse(Response response) throws IOException {
    if (response.body() == null) {
      return response;
    }
    String body = slurp(response.body().asReader());
    return Response.create(response.status(), response.reason(), response.headers(), body, UTF_8);
  }

  @Override
  public Exception decode(String methodKey, Response response) {
    try {
      // in case of error parsing, we can access the original contents.
      response = bufferResponse(response);
      UltraDNSError error = UltraDNSError.class.cast(decoder.decode(response, UltraDNSError.class));
      if (error == null) {
        return FeignException.errorStatus(methodKey, response);
      }
      String message = format("%s failed", methodKey);
      if (error.code != -1) {
        message = format("%s with error %s", message, error.code);
      }
      if (error.description != null) {
        message = format("%s: %s", message, error.description);
      }
      if (error.code == UltraDNSException.SYSTEM_ERROR) {
        return new RetryableException(message, null);
      }
      return new UltraDNSException(message, error.code);
    } catch (IOException ignored) {
      return FeignException.errorStatus(methodKey, response);
    } catch (Exception propagate) {
      return propagate;
    }
  }

  static class UltraDNSError extends DefaultHandler
      implements ContentHandlerWithResult<UltraDNSError> {

    private StringBuilder currentText = new StringBuilder();
    private int code = -1;
    private String description;

    @Inject
    UltraDNSError() {
    }

    @Override
    public UltraDNSError result() {
      return (code == -1 && description == null) ? null : this;
    }

    @Override
    public void endElement(String uri, String name, String qName) {
      if (qName.endsWith("errorCode")) {
        code = Integer.parseInt(currentText.toString().trim());
      } else if (qName.endsWith("errorDescription") || qName.endsWith("faultstring")) {
        description = currentText.toString().trim();
        if ("".equals(description)) {
          description = null;
        }
      }
      currentText = new StringBuilder();
    }

    @Override
    public void characters(char ch[], int start, int length) {
      currentText.append(ch, start, length);
    }
  }
}
