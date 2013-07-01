package denominator.dynect;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterators.transform;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import denominator.dynect.DynECT.Record;
import denominator.dynect.DynECTException.Message;
import denominator.model.Zone;
import feign.RetryableException;
import feign.codec.Decoder;

/**
 * this will propagate exceptions when they are mistakenly sent with a 200 error
 * code.
 */
class DynECTDecoder extends feign.codec.Decoder {

    static Decoder login() {
        return parseDataWith(TokenDecoder.INSTANCE);
    }

    private static enum TokenDecoder implements Function<JsonReader, String> {
        INSTANCE;

        @Override
        public String apply(JsonReader reader) {
            return new JsonParser().parse(reader).getAsJsonObject().get("token").getAsString();
        }
    }

    static Decoder resourceRecordSets() {
        return parseDataWith(new ResourceRecordSetsDecoder());
    }

    static Decoder zones() {
        return parseDataWith(ZonesDecoder.INSTANCE);
    }

    private static enum ZonesDecoder implements Function<JsonReader, List<Zone>> {
        INSTANCE;

        @Override
        public List<Zone> apply(JsonReader reader) {
            Iterator<JsonElement> data = new JsonParser().parse(reader).getAsJsonArray().iterator();
            return ImmutableList.copyOf(transform(data,
                    compose(ToZone.INSTANCE, firstGroupFunction("/REST.*/([^/]+)/?$"))));
        }
    }

    private static enum ToZone implements Function<String, Zone> {
        INSTANCE;
        @Override
        public Zone apply(String input) {
            return Zone.create(input);
        }

        @Override
        public String toString() {
            return "Zone";
        }
    };

    public static Decoder recordIds() {
        return parseDataWith(RecordIdsDecoder.INSTANCE);
    }

    private static enum RecordIdsDecoder implements Function<JsonReader, List<String>> {
        INSTANCE;

        @Override
        public List<String> apply(JsonReader reader) {
            Iterator<JsonElement> data = new JsonParser().parse(reader).getAsJsonArray().iterator();
            return ImmutableList.copyOf(transform(data,
                    firstGroupFunction("/REST/([a-zA-Z]+Record/[^\"]+/[^\"]+/[0-9]+)")));
        }
    }

    static Decoder records() {
        return parseDataWith(RecordsByNameAndTypeDecoder.INSTANCE);
    }

    private static enum RecordsByNameAndTypeDecoder implements Function<JsonReader, Iterator<Record>> {
        INSTANCE;

        @Override
        public Iterator<Record> apply(JsonReader reader) {
            Iterator<JsonElement> data = new JsonParser().parse(reader).getAsJsonArray().iterator();
            return transform(data, ToRecord.INSTANCE);
        }
    }

    static Decoder parseDataWith(Function<JsonReader, ?> fn) {
        return new DynECTDecoder(fn);
    }

    private final Function<JsonReader, ?> fn;

    DynECTDecoder(Function<JsonReader, ?> fn) {
        this.fn = fn;
    }

    @Override
    public Object decode(String methodKey, Reader ireader, TypeToken<?> ignored) throws Throwable {
        JsonReader reader = new JsonReader(ireader);
        try {
            ImmutableList.Builder<Message> messages = ImmutableList.<Message> builder();
            String status = "failed";
            // sometimes the response starts with an array
            if (reader.peek() == JsonToken.BEGIN_ARRAY)
                reader.beginArray();
            reader.beginObject();
            while (reader.hasNext()) {
                String nextName = reader.nextName();
                if ("status".equals(nextName)) {
                    status = reader.nextString();
                } else if ("success".equals(status) && "data".equals(nextName)) {
                    return fn.apply(reader);
                } else if ("msgs".equals(nextName)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        Message message = new Message();
                        while (reader.hasNext()) {
                            String fieldName = reader.nextName();
                            if ("INFO".equals(fieldName)) {
                                message.info = reader.nextString();
                            } else if ("ERR_CD".equals(fieldName) && reader.peek() != JsonToken.NULL) {
                                message.code = Optional.fromNullable(reader.nextString());
                            } else {
                                reader.skipValue();
                            }
                        }
                        messages.add(message);
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            if ("incomplete".equals(status))
                throw new RetryableException(messages.build().toString(), null);
            throw new DynECTException(methodKey, status, messages.build());
        } finally {
            reader.close();
        }
    }

    private static Function<JsonElement, String> firstGroupFunction(String pattern) {

        final Pattern compiled = Pattern.compile(pattern);
        return new Function<JsonElement, String>() {

            public String apply(JsonElement in) {
                Matcher matcher = compiled.matcher(in.getAsString());
                checkState(matcher.find() && matcher.groupCount() == 1, "%s didn't match %s", in, compiled);
                return matcher.group(1);
            }
        };
    }

}
