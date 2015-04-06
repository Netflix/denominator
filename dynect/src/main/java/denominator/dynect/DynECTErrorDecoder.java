package denominator.dynect;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import denominator.dynect.DynECTException.Message;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import static java.lang.System.currentTimeMillis;

class DynECTErrorDecoder implements ErrorDecoder {

  private final AtomicReference<Boolean> sessionValid;

  @Inject
  DynECTErrorDecoder(AtomicReference<Boolean> sessionValid) {
    this.sessionValid = sessionValid;
  }

  @Override
  public Exception decode(String methodKey, Response response) {
    try {
      JsonReader reader = new JsonReader(response.body().asReader());
      List<Message> messages = new ArrayList<Message>();
      // TODO: checked with Ryan at DynECT to see if this is still needed
      // formerly required when error response is wrapped in an array.
      if (reader.peek() == JsonToken.BEGIN_ARRAY) {
        reader.beginArray();
      }
      String status = "failed";
      reader.beginObject();
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if ("status".equals(nextName)) {
          status = reader.nextString();
        } else if ("msgs".equals(nextName)) {
          reader.beginArray();
          while (reader.hasNext()) {
            reader.beginObject();
            Message message = new Message();
            while (reader.hasNext()) {
              String fieldName = reader.nextName();
              if ("INFO".equals(fieldName)) {
                message.info = reader.nextString();
              } else if ("ERR_CD".equals(fieldName) && reader.peek() != JsonToken.NULL) {
                message.code = reader.nextString();
              } else {
                reader.skipValue();
              }
            }
            messages.add(message);
            reader.endObject();
          }
          reader.endArray();
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      reader.close();
      DynECTException cause = new DynECTException(status, messages);
      if ("incomplete".equals(status)) {
        return new RetryableException(messages.toString(), cause, null);
      } else if (!messages.isEmpty()) {
        for (Message message : messages) {
          if ("token: This session already has a job running".equals(message.info())) {
            return new RetryableException(messages.toString(), cause, null);
          } else if ("zone: Operation blocked by current task".equals(message.info())) {
            // Tasks are not exposed so the only thing we can do is wait a relatively long time.
            Date retryAfter = new Date(currentTimeMillis() + 1000);
            return new RetryableException(messages.toString(), cause, retryAfter);
          } else if ("login: IP address does not match current session".equals(message.info())) {
            sessionValid.set(false);
            return new RetryableException(messages.toString(), cause, null);
          }
        }
      }
      return new DynECTException(status, messages);
    } catch (IOException ignored) {
      return FeignException.errorStatus(methodKey, response);
    } catch (Exception propagate) {
      return propagate;
    }
  }
}
