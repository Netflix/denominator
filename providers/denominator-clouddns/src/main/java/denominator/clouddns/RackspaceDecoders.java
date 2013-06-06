package denominator.clouddns;

import static com.google.common.collect.Ordering.usingToString;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Record;
import denominator.model.Zone;
import feign.codec.Decoder;

class RackspaceDecoders {
    static class DomainListDecoder extends ListWithNextDecoder<Zone> {
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

    static class RecordListDecoder extends ListWithNextDecoder<Record> {
        @Override
        protected String jsonKey() {
            return "records";
        }

        protected Record build(JsonReader reader) throws IOException {
            Record record = new Record();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals("name")) {
                    record.name = reader.nextString();
                } else if (key.equals("type")) {
                    record.type = reader.nextString();
                } else if (key.equals("ttl")) {
                    record.ttl = reader.nextInt();
                } else if (key.equals("data")) {
                    record.data = reader.nextString();
                } else if (key.equals("priority")) {
                    record.priority = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
            return record;
        }
    }

    private static abstract class ListWithNextDecoder<X> extends Decoder {
        protected abstract String jsonKey();

        protected abstract X build(JsonReader reader) throws IOException;

        @Override
        protected ListWithNext<X> decode(String methodKey, Reader ireader, TypeToken<?> type) throws Throwable {
            Builder<X> builder = ImmutableList.<X> builder();
            String nextUrl = null;
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
                        if ("next".equals(currentRel))
                            nextUrl = currentHref;
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();
            ListWithNext<X> records = new ListWithNext<X>();
            records.records = usingToString().sortedCopy(builder.build());
            if (nextUrl != null) {
                records.next = URI.create(nextUrl);
            }
            return records;
        }
    }
}