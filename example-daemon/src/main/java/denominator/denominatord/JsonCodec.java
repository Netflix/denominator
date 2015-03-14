package denominator.denominatord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

class JsonCodec {

  private final Gson json;

  JsonCodec() {
    this.json = new GsonBuilder().setPrettyPrinting().create();
  }

  <T> T readJson(RecordedRequest request, Class<T> clazz) {
    return json.fromJson(request.getUtf8Body(), clazz);
  }

  <T> MockResponse toJsonArray(Iterator<T> elements) {
    elements.hasNext(); // defensive to make certain error cases eager.

    StringWriter out = new StringWriter(); // MWS cannot do streaming responses.
    try {
      JsonWriter writer = new JsonWriter(out);
      writer.setIndent("  ");
      writer.beginArray();
      while (elements.hasNext()) {
        Object next = elements.next();
        json.toJson(next, next.getClass(), writer);
      }
      writer.endArray();
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new MockResponse().setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(out.toString() + "\n"); // curl nice
  }
}
