package denominator.designate;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import denominator.designate.Designate.Record;
import denominator.model.Zone;

import static com.google.gson.stream.JsonToken.NULL;

class DesignateAdapters {

  private static final Comparator<Object> TO_STRING_COMPARATOR = new Comparator<Object>() {
    @Override
    public int compare(Object left, Object right) {
      return left.toString().compareTo(right.toString());
    }
  };

  static Record buildRecord(JsonReader reader) throws IOException {
    Record record = new Record();
    while (reader.hasNext()) {
      String key = reader.nextName();
      if (key.equals("id")) {
        record.id = reader.nextString();
      } else if (key.equals("name")) {
        record.name = reader.nextString();
      } else if (key.equals("type")) {
        record.type = reader.nextString();
      } else if (key.equals("ttl") && reader.peek() != NULL) {
        record.ttl = reader.nextInt();
      } else if (key.equals("data")) {
        record.data = reader.nextString();
      } else if (key.equals("priority") && reader.peek() != NULL) {
        record.priority = reader.nextInt();
      } else {
        reader.skipValue();
      }
    }
    return record;
  }

  @SuppressWarnings("unchecked")
  private static <X> Comparator<X> toStringComparator() {
    return Comparator.class.cast(TO_STRING_COMPARATOR);
  }

  static class RecordAdapter extends TypeAdapter<Record> {

    @Override
    public void write(JsonWriter out, Record record) throws IOException {
      out.beginObject();
      out.name("name").value(record.name);
      out.name("type").value(record.type);
      if (record.ttl != null) {
        out.name("ttl").value(record.ttl);
      }
      out.name("data").value(record.data);
      if (record.priority != null) {
        out.name("priority").value(record.priority);
      }
      out.endObject();
    }

    @Override
    public Record read(JsonReader reader) throws IOException {
      reader.beginObject();
      Record record = buildRecord(reader);
      reader.endObject();
      return record;
    }
  }

  static class DomainListAdapter extends ListAdapter<Zone> {

    @Override
    protected String jsonKey() {
      return "domains";
    }

    protected Zone build(JsonReader reader) throws IOException {
      String name = null, id = null, email = null;
      int ttl = -1;
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if (nextName.equals("id")) {
          id = reader.nextString();
        } else if (nextName.equals("name")) {
          name = reader.nextString();
        } else if (nextName.equals("ttl")) {
          ttl = reader.nextInt();
        } else if (nextName.equals("email")) {
          email = reader.nextString();
        } else {
          reader.skipValue();
        }
      }
      return Zone.create(id, name, ttl, email);
    }
  }

  static class RecordListAdapter extends ListAdapter<Record> {

    @Override
    protected String jsonKey() {
      return "records";
    }

    protected Record build(JsonReader reader) throws IOException {
      return buildRecord(reader);
    }
  }

  static abstract class ListAdapter<X> extends TypeAdapter<List<X>> {

    protected abstract String jsonKey();

    protected abstract X build(JsonReader reader) throws IOException;

    @Override
    public List<X> read(JsonReader reader) throws IOException {
      List<X> elements = new LinkedList<X>();
      reader.beginObject();
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if (jsonKey().equals(nextName)) {
          reader.beginArray();
          while (reader.hasNext()) {
            reader.beginObject();
            elements.add(build(reader));
            reader.endObject();
          }
          reader.endArray();
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      Collections.sort(elements, toStringComparator());
      return elements;
    }

    @Override
    public void write(JsonWriter out, List<X> value) throws IOException {
      throw new UnsupportedOperationException();
    }
  }
}
