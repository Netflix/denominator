package denominator.ultradns;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.google.gson.stream.JsonToken;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.log4j.Logger;

class UltraDNSRestErrorDecoder implements ErrorDecoder {

  private AtomicReference<Boolean> sessionValid;
  private static final Logger logger = Logger.getLogger(UltraDNSRestErrorDecoder.class);

  @Inject
  UltraDNSRestErrorDecoder(AtomicReference<Boolean> sessionValid) {
    this.sessionValid = sessionValid;
  }

  @Override
  public Exception decode(String methodKey, Response response) {
    logger.info("Decoding Error .......");
    try {
      JsonReader reader = new JsonReader(response.body().asReader());
      UltraDNSRestException.Message message = new UltraDNSRestException.Message();

      if (reader.peek() == JsonToken.BEGIN_ARRAY) {
        reader.beginArray();
      }
      reader.beginObject();
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if ("errorCode".equals(nextName)) {
          message.setErrorCode(Integer.parseInt(reader.nextString()));
        } else if ("errorMessage".equals(nextName)) {
          message.setErrorMessage(reader.nextString());
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      reader.close();
      return new UltraDNSRestException(message.errorMessage, message.errorCode);
    } catch (IOException ignored) {
      return FeignException.errorStatus(methodKey, response);
    } catch (Exception propagate) {
      return propagate;
    }
  }
}
