package denominator.clouddns;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;

import denominator.clouddns.RackspaceApis.JobIdAndStatus;
import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Record;
import denominator.model.Zone;

class RackspaceAdapters {

  private static final Comparator<Object> TO_STRING_COMPARATOR = new Comparator<Object>() {
    @Override
    public int compare(Object left, Object right) {
      return left.toString().compareTo(right.toString());
    }
  };

  @SuppressWarnings("unchecked")
  private static <X> Comparator<X> toStringComparator() {
    return Comparator.class.cast(TO_STRING_COMPARATOR);
  }

  static class JobIdAndStatusAdapter extends TypeAdapter<JobIdAndStatus> {

    @Override
    public void write(JsonWriter out, JobIdAndStatus value) throws IOException {
      // never need to write this object
    }

    @Override
    public JobIdAndStatus read(JsonReader reader) throws IOException {
      JobIdAndStatus jobIdAndStatus = new JobIdAndStatus();

      reader.beginObject();

      while (reader.hasNext()) {
        String key = reader.nextName();
        if (key.equals("jobId")) {
          jobIdAndStatus.id = reader.nextString();
        } else if (key.equals("status")) {
          jobIdAndStatus.status = reader.nextString();
        } else {
          reader.skipValue();
        }
      }

      reader.endObject();

      return jobIdAndStatus;
    }
  }

  static class DomainListAdapter extends ListWithNextAdapter<Zone> {

    @Override
    protected String jsonKey() {
      return "domains";
    }

    protected Zone build(JsonReader reader) throws IOException {
      String name = null;
      String id = null;
      while (reader.hasNext()) {
        String key = reader.nextName();
        if (key.equals("name")) {
          name = reader.nextString();
        } else if (key.equals("id")) {
          id = reader.nextString();
        } else {
          reader.skipValue();
        }
      }
      return Zone.create(name, id);
    }
  }

  static class RecordListAdapter extends ListWithNextAdapter<Record> {

    @Override
    protected String jsonKey() {
      return "records";
    }

    protected Record build(JsonReader reader) throws IOException {
      Record record = new Record();
      while (reader.hasNext()) {
        String key = reader.nextName();
        if (key.equals("id")) {
          record.id = reader.nextString();
        } else if (key.equals("name")) {
          record.name = reader.nextString();
        } else if (key.equals("type")) {
          record.type = reader.nextString();
        } else if (key.equals("ttl")) {
          record.ttl = reader.nextInt();
        } else if (key.equals("data")) {
          record.data(reader.nextString());
        } else if (key.equals("priority")) {
          record.priority = reader.nextInt();
        } else {
          reader.skipValue();
        }
      }
      return record;
    }
  }

  static abstract class ListWithNextAdapter<X> extends TypeAdapter<ListWithNext<X>> {

    protected abstract String jsonKey();

    protected abstract X build(JsonReader reader) throws IOException;

    @Override
    public ListWithNext<X> read(JsonReader reader) throws IOException {
      ListWithNext<X> records = new ListWithNext<X>();
      reader.beginObject();
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if (jsonKey().equals(nextName)) {
          reader.beginArray();
          while (reader.hasNext()) {
            reader.beginObject();
            records.add(build(reader));
            reader.endObject();
          }
          reader.endArray();
        } else if ("links".equals(nextName)) {
          reader.beginArray();
          while (reader.hasNext()) {
            String currentRel = null;
            String currentHref = null;
            reader.beginObject();
            while (reader.hasNext()) {
              String key = reader.nextName();
              if ("rel".equals(key)) {
                currentRel = reader.nextString();
              } else if ("href".equals(key)) {
                currentHref = reader.nextString();
              } else {
                reader.skipValue();
              }
            }
            if ("next".equals(currentRel)) {
              if (currentHref != null) {
                records.next = URI.create(currentHref);
              }
            }
            reader.endObject();
          }
          reader.endArray();
        } else {
          reader.skipValue();
        }
      }
      reader.endObject();
      Collections.sort(records, toStringComparator());
      return records;
    }

    @Override
    public void write(JsonWriter out, ListWithNext<X> value) throws IOException {
      throw new UnsupportedOperationException();
    }
  }
}
