package denominator.designate;

import static com.google.common.collect.Ordering.usingToString;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import denominator.designate.OpenStackApis.Record;
import denominator.model.Zone;
import feign.codec.Decoder;

class OpenStackDecoders {
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
            } else if (key.equals("ttl") && reader.peek() != JsonToken.NULL) {
                record.ttl = reader.nextInt();
            } else if (key.equals("data")) {
                record.data = reader.nextString();
            } else if (key.equals("priority") && reader.peek() != JsonToken.NULL) {
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
            Builder<X> builder = ImmutableList.<X> builder();
            JsonReader reader = new JsonReader(ireader);
            reader.beginObject();
            while (reader.hasNext()) {
                String nextName = reader.nextName();
                if (jsonKey().equals(nextName)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        builder.add(build(reader));
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
            return usingToString().sortedCopy(builder.build());
        }
    }
}
