package denominator.dynect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import denominator.dynect.DynECT.Data;
import denominator.dynect.DynECT.Record;
import feign.RetryableException;

import static denominator.common.Preconditions.checkState;

/**
 * DynECT json includes an envelope called "data". These type adapters apply at that level.
 */
class DynECTAdapters {

  private static List<String> toFirstGroup(String pattern, JsonArray elements) {
    Pattern compiled = Pattern.compile(pattern);
    List<String> results = new ArrayList<String>(elements.size());
    for (JsonElement in : elements) {
      Matcher matcher = compiled.matcher(in.getAsString());
      checkState(matcher.find() && matcher.groupCount() == 1, "%s didn't match %s", in, compiled);
      results.add(matcher.group(1));
    }
    return results;
  }

  static class NothingForbiddenAdapter extends DataAdapter<Boolean> {

    @Override
    public Boolean build(JsonReader reader) throws IOException {
      return
          new JsonParser().parse(reader).getAsJsonObject().get("forbidden").getAsJsonArray().size()
          == 0;
    }
  }

  static class TokenAdapter extends DataAdapter<String> {

    @Override
    public String build(JsonReader reader) throws IOException {
      return new JsonParser().parse(reader).getAsJsonObject().get("token").getAsString();
    }
  }

  static class ZoneNamesAdapter extends DataAdapter<List<String>> {

    @Override
    public List<String> build(JsonReader reader) throws IOException {
      JsonArray data = new JsonParser().parse(reader).getAsJsonArray();
      return toFirstGroup("/REST.*/([^/]+)/?$", data);
    }
  }

  static class RecordsByNameAndTypeAdapter extends DataAdapter<Iterator<Record>> {

    @Override
    public Iterator<Record> build(JsonReader reader) throws IOException {
      JsonArray data = new JsonParser().parse(reader).getAsJsonArray();
      List<Record> records = new ArrayList<Record>();
      for (JsonElement datum : data) {
        records.add(ToRecord.INSTANCE.apply(datum));
      }
      return records.iterator();
    }
  }

  static abstract class DataAdapter<X> extends TypeAdapter<Data<X>> {

    protected abstract X build(JsonReader reader) throws IOException;

    @Override
    public Data<X> read(JsonReader reader) throws IOException {
      Data<X> data = new Data<X>();
      String status = null;
      reader.beginObject();
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if ("data".equals(nextName) && reader.peek() != JsonToken.NULL) {
          try {
            data.data = build(reader);
          } catch (JsonIOException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
              throw IOException.class.cast(e.getCause());
            }
            throw e;
          }
        } else if ("status".equals(nextName)) {
          status = reader.nextString();
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      if ("incomplete".equals(status)) {
        throw new RetryableException(status, null);
      }
      return data;
    }

    @Override
    public void write(JsonWriter out, Data<X> value) throws IOException {
      throw new UnsupportedOperationException();
    }
  }
}
