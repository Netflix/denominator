package denominator.dynect;

import static com.google.common.collect.Iterators.peekingIterator;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.PeekingIterator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import denominator.dynect.DynECT.Record;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

class ResourceRecordSetsDecoder implements Function<JsonReader, Iterator<ResourceRecordSet<?>>> {
    @Override
    public Iterator<ResourceRecordSet<?>> apply(JsonReader reader) {
        JsonParser parser = new JsonParser();
        JsonElement data = parser.parse(reader);

        // there are 2 forms for record responses: an array of same type, or a
        // map per type.
        if (data.isJsonArray()) {
            return new GroupByRecordNameAndTypeIterator(data.getAsJsonArray().iterator());
        } else if (data.isJsonObject()) {
            return new GroupByRecordNameAndTypeIterator(FluentIterable.from(data.getAsJsonObject().entrySet())
                    .transformAndConcat(GetValue.INSTANCE).iterator());
        } else {
            throw new IllegalStateException("unknown format: " + data);
        }
    }

    private static enum GetValue implements Function<Map.Entry<String, JsonElement>, Iterable<JsonElement>> {
        INSTANCE;
        @Override
        public Iterable<JsonElement> apply(Entry<String, JsonElement> input) {
            return input.getValue().getAsJsonArray();
        }
    }

    private static class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

        private final PeekingIterator<JsonElement> peekingIterator;

        public GroupByRecordNameAndTypeIterator(Iterator<JsonElement> sortedIterator) {
            this.peekingIterator = peekingIterator(sortedIterator);
        }

        @Override
        public boolean hasNext() {
            return peekingIterator.hasNext();
        }

        @Override
        public ResourceRecordSet<?> next() {
            JsonElement current = peekingIterator.next();
            Record record = ToRecord.INSTANCE.apply(current);
            Builder<Map<String, Object>> builder = ResourceRecordSet.builder().name(record.name).type(record.type)
                    .ttl(record.ttl).add(record.rdata);
            while (hasNext()) {
                JsonElement next = peekingIterator.peek();
                if (next == null || next.isJsonNull())
                    continue;
                if (fqdnAndTypeEquals(current.getAsJsonObject(), next.getAsJsonObject())) {
                    peekingIterator.next();
                    builder.add(ToRecord.INSTANCE.apply(next).rdata);
                } else {
                    break;
                }
            }
            return builder.build();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean fqdnAndTypeEquals(JsonObject current, JsonObject next) {
        return current.get("fqdn").equals(next.get("fqdn"))
                && current.get("record_type").equals(next.get("record_type"));
    }
}
