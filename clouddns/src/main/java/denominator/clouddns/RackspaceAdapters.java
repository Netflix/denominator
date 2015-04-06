package denominator.clouddns;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;

import denominator.clouddns.RackspaceApis.Job;
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

  static class JobAdapter extends TypeAdapter<Job> {

    @Override
    public void write(JsonWriter out, Job value) throws IOException {
      // never need to write this object
    }

    @Override
    public Job read(JsonReader reader) throws IOException {
      Job job = new Job();

      reader.beginObject();
      while (reader.hasNext()) {
        String key = reader.nextName();
        if (key.equals("jobId")) {
          job.id = reader.nextString();
          // Hunt for an id in the response
          //   * response.*[0].id
        } else if (key.equals("response")) {
          reader.beginObject();
          while (reader.hasNext()) {
            reader.nextName(); // skip name
            reader.beginArray();
            reader.beginObject();
            while (reader.hasNext()) {
              key = reader.nextName();
              if (key.equals("id")) {
                job.resultId = reader.nextString();
              } else {
                reader.skipValue();
              }
            }
            reader.endObject();
            reader.endArray();
          }
          reader.endObject();
        } else if (key.equals("status")) {
          job.status = reader.nextString();
          // There are two places to find a useful error message
          //   * error.failedItems.faults[0].details <- on delete
          //   * error.details <- on create
        } else if (key.equals("error")) {
          reader.beginObject();
          while (reader.hasNext()) {
            key = reader.nextName();
            if (key.equals("failedItems")) {
              reader.beginObject();
              reader.nextName();
              reader.beginArray();
              reader.beginObject();
              while (reader.hasNext()) {
                key = reader.nextName();
                if (key.equals("details")) {
                  job.errorDetails = reader.nextString();
                } else {
                  reader.skipValue();
                }
              }
              reader.endObject();
              reader.endArray();
              reader.endObject();
            } else if (key.equals("details") && job.errorDetails == null) {
              job.errorDetails = reader.nextString();
            } else {
              reader.skipValue();
            }
          }
          reader.endObject();
        } else {
          reader.skipValue();
        }
      }

      reader.endObject();

      return job;
    }
  }

  static class DomainListAdapter extends ListWithNextAdapter<Zone> {

    @Override
    protected String jsonKey() {
      return "domains";
    }

    protected Zone build(JsonReader reader) throws IOException {
      String name = null, id = null, email = null;
      while (reader.hasNext()) {
        String nextName = reader.nextName();
        if (nextName.equals("id")) {
          id = reader.nextString();
        } else if (nextName.equals("name")) {
          name = reader.nextString();
        } else if (nextName.equals("emailAddress")) {
          email = reader.nextString();
        } else {
          reader.skipValue();
        }
      }
      return Zone.create(id, name, /* CloudDNS doesn't return ttl in the list api. */ 0, email);
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
