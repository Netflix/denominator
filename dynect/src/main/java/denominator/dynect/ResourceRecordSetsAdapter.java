package denominator.dynect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import denominator.common.PeekingIterator;
import denominator.dynect.DynECT.Record;
import denominator.dynect.DynECTAdapters.DataAdapter;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

import static denominator.common.Util.peekingIterator;

class ResourceRecordSetsAdapter extends DataAdapter<Iterator<ResourceRecordSet<?>>> {

  private static boolean fqdnAndTypeEquals(JsonObject current, JsonObject next) {
    return current.get("fqdn").equals(next.get("fqdn"))
           && current.get("record_type").equals(next.get("record_type"));
  }

  @Override
  public Iterator<ResourceRecordSet<?>> build(JsonReader reader) throws IOException {
    JsonElement data;
    try {
      data = new JsonParser().parse(reader);
    } catch (JsonIOException e) {
      if (e.getCause() != null && e.getCause() instanceof IOException) {
        throw IOException.class.cast(e.getCause());
      }
      throw e;
    }

    // there are 2 forms for record responses: an array of same type, or a
    // map per type.
    if (data.isJsonArray()) {
      return new GroupByRecordNameAndTypeIterator(data.getAsJsonArray().iterator());
    } else if (data.isJsonObject()) {
      List<JsonElement> elements = new ArrayList<JsonElement>();
      for (Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
        if (entry.getValue() instanceof JsonArray) {
          for (JsonElement element : entry.getValue().getAsJsonArray()) {
            elements.add(element);
          }
        }
      }
      return new GroupByRecordNameAndTypeIterator(elements.iterator());
    } else {
      throw new IllegalStateException("unknown format: " + data);
    }
  }

  static class GroupByRecordNameAndTypeIterator extends PeekingIterator<ResourceRecordSet<?>> {

    private final PeekingIterator<JsonElement> peekingIterator;

    public GroupByRecordNameAndTypeIterator(Iterator<JsonElement> sortedIterator) {
      this.peekingIterator = peekingIterator(sortedIterator);
    }

    @Override
    protected ResourceRecordSet<?> computeNext() {
      if (!peekingIterator.hasNext()) {
        return endOfData();
      }
      JsonElement current = peekingIterator.next();
      Record record = ToRecord.INSTANCE.apply(current);
      Builder<Map<String, Object>>
          builder =
          ResourceRecordSet.builder().name(record.name).type(record.type)
              .ttl(record.ttl).add(record.rdata);
      while (peekingIterator.hasNext()) {
        JsonElement next = peekingIterator.peek();
        if (next == null || next.isJsonNull()) {
          continue;
        }
        if (fqdnAndTypeEquals(current.getAsJsonObject(), next.getAsJsonObject())) {
          peekingIterator.next();
          builder.add(ToRecord.INSTANCE.apply(next).rdata);
        } else {
          break;
        }
      }
      return builder.build();
    }
  }
}
