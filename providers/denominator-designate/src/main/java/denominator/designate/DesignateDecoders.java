package denominator.designate;

import static com.google.gson.stream.JsonToken.NULL;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.stream.JsonReader;

import denominator.designate.Designate.Record;
import denominator.model.Zone;
import feign.codec.Decoder;

class DesignateDecoders {
    static class DomainListDecoder extends ListDecoder<Zone> {
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

    static class RecordDecoder extends Decoder {
        @Override
        public Record decode(String methodKey, Reader ireader, Type type) throws Throwable {
            JsonReader reader = new JsonReader(ireader);
            reader.beginObject();
            Record record = buildRecord(reader);
            reader.endObject();
            reader.close();
            return record;
        }
    }

    static class RecordListDecoder extends ListDecoder<Record> {
        @Override
        protected String jsonKey() {
            return "records";
        }

        protected Record build(JsonReader reader) throws IOException {
            return buildRecord(reader);
        }
    }

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

    private static abstract class ListDecoder<X> extends Decoder {
        protected abstract String jsonKey();

        protected abstract X build(JsonReader reader) throws IOException;

        @Override
        public List<X> decode(String methodKey, Reader ireader, Type type) throws Throwable {
            List<X> elements = new LinkedList<X>();
            JsonReader reader = new JsonReader(ireader);
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
            reader.close();
            Collections.sort(elements, toStringComparator());
            return elements;
        }
    }

    @SuppressWarnings("unchecked")
    private static <X> Comparator<X> toStringComparator() {
        return Comparator.class.cast(TO_STRING_COMPARATOR);
    }

    private static final Comparator<Object> TO_STRING_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(Object left, Object right) {
            return left.toString().compareTo(right.toString());
        }
    };
}
