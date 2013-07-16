package denominator.clouddns;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;

import javax.inject.Inject;

import com.google.gson.stream.JsonReader;

import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Record;
import denominator.model.Zone;
import feign.codec.Decoder;

class RackspaceDecoders {
    static class DomainListDecoder extends ListWithNextDecoder<Zone> {
        @Inject
        DomainListDecoder() {
        }

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
        @Inject
        RecordListDecoder() {
        }

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

    static abstract class ListWithNextDecoder<X> implements Decoder.TextStream<ListWithNext<X>> {
        protected abstract String jsonKey();

        protected abstract X build(JsonReader reader) throws IOException;

        @Override
        public ListWithNext<X> decode(Reader ireader, Type type) throws IOException {
            ListWithNext<X> records = new ListWithNext<X>();
            JsonReader reader = new JsonReader(ireader);
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
            reader.close();
            Collections.sort(records, toStringComparator());
            return records;
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