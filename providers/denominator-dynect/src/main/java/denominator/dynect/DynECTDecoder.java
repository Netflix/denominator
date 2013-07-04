package denominator.dynect;

import static denominator.common.Preconditions.checkState;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
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

    static Decoder login(AtomicReference<Boolean> sessionValid) {
        return parseDataWith(sessionValid, TokenDecoder.INSTANCE);
    }

    interface Parser<T> {
        T apply(JsonReader reader);
    }

    private static enum TokenDecoder implements Parser<String> {
        INSTANCE;

        @Override
        public String apply(JsonReader reader) {
            return new JsonParser().parse(reader).getAsJsonObject().get("token").getAsString();
        }
    }

    static Decoder resourceRecordSets(AtomicReference<Boolean> sessionValid) {
        return parseDataWith(sessionValid, new ResourceRecordSetsDecoder());
    }

    static Decoder zones(AtomicReference<Boolean> sessionValid) {
        return parseDataWith(sessionValid, ZonesDecoder.INSTANCE);
    }

    private static enum ZonesDecoder implements Parser<List<Zone>> {
        INSTANCE;

        @Override
        public List<Zone> apply(JsonReader reader) {
            List<Zone> zones = new ArrayList<Zone>();
            JsonArray data = new JsonParser().parse(reader).getAsJsonArray();
            for (String name : toFirstGroup("/REST.*/([^/]+)/?$", data)) {
                zones.add(Zone.create(name));
            }
            return zones;
        }
    }

    public static Decoder recordIds(AtomicReference<Boolean> sessionValid) {
        return parseDataWith(sessionValid, RecordIdsDecoder.INSTANCE);
    }

    private static enum RecordIdsDecoder implements Parser<List<String>> {
        INSTANCE;

        @Override
        public List<String> apply(JsonReader reader) {
            JsonArray data = new JsonParser().parse(reader).getAsJsonArray();
            return toFirstGroup("/REST/([a-zA-Z]+Record/[^\"]+/[^\"]+/[0-9]+)", data);
        }
    }

    static Decoder records(AtomicReference<Boolean> sessionValid) {
        return parseDataWith(sessionValid, RecordsByNameAndTypeDecoder.INSTANCE);
    }

    private static enum RecordsByNameAndTypeDecoder implements Parser<Iterator<Record>> {
        INSTANCE;

        @Override
        public Iterator<Record> apply(JsonReader reader) {
            List<Record> records = new ArrayList<Record>();
            for (JsonElement data : new JsonParser().parse(reader).getAsJsonArray()) {
                records.add(ToRecord.INSTANCE.apply(data));
            }
            return records.iterator();
        }
    }

    static Decoder parseDataWith(AtomicReference<Boolean> sessionValid, Parser<?> fn) {
        return new DynECTDecoder(sessionValid, fn);
    }

    private final AtomicReference<Boolean> sessionValid;
    private final Parser<?> fn;

    DynECTDecoder(AtomicReference<Boolean> sessionValid, Parser<?> fn) {
        this.sessionValid = sessionValid;
        this.fn = fn;
    }

    @Override
    public Object decode(String methodKey, Reader ireader, Type ignored) throws Throwable {
        JsonReader reader = new JsonReader(ireader);
        try {
            List<Message> messages = new ArrayList<Message>();
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
                                message.code = reader.nextString();
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
            if ("incomplete".equals(status)) {
                throw new RetryableException(messages.toString(), null);
            } else if (!messages.isEmpty() ){
                for (Message message : messages) {
                    if ("token: This session already has a job running".equals(message.info())) {
                        throw new RetryableException(messages.toString(), null);
                    } else if ("login: IP address does not match current session".equals(message.info())) {
                        sessionValid.set(false);
                        throw new RetryableException(messages.toString(), null);
                    }
                }
            }
            throw new DynECTException(methodKey, status, messages);
        } finally {
            reader.close();
        }
    }

    private static List<String> toFirstGroup(String pattern, JsonArray elements) {
        Pattern compiled = Pattern.compile(pattern);
        List<String> results = new ArrayList<String>(elements.size());
        for (JsonElement in : elements) {
            Matcher matcher = compiled.matcher(in.getAsString());
            checkState(matcher.find() && matcher.groupCount() == 1, "%s didn't match %s", in, compiled);
            results.add(matcher.group(1));
        }
        return results;
    }
}
