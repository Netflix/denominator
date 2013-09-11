package denominator.dynect;

import static denominator.common.Preconditions.checkState;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import denominator.dynect.DynECT.Record;
import denominator.model.Zone;
import feign.codec.Decoder;

class DynECTDecoder<T> implements Decoder.TextStream<T> {

    interface Parser<T> {
        T apply(JsonReader reader) throws IOException;
    }

    static enum NothingForbiddenDecoder implements Parser<Boolean> {
        INSTANCE;

        @Override
        public Boolean apply(JsonReader reader) throws IOException {
            try {
                return new JsonParser().parse(reader).getAsJsonObject().get("forbidden").getAsJsonArray().size() == 0;
            } catch (JsonIOException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            }
        }
    }

    static enum TokenDecoder implements Parser<String> {
        INSTANCE;

        @Override
        public String apply(JsonReader reader) throws IOException {
            try {
                return new JsonParser().parse(reader).getAsJsonObject().get("token").getAsString();
            } catch (JsonIOException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            }
        }
    }

    static enum ZonesDecoder implements Parser<List<Zone>> {
        INSTANCE;

        @Override
        public List<Zone> apply(JsonReader reader) throws IOException {
            JsonArray data;
            try {
                data = new JsonParser().parse(reader).getAsJsonArray();
            } catch (JsonIOException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            }
            List<Zone> zones = new ArrayList<Zone>();
            for (String name : toFirstGroup("/REST.*/([^/]+)/?$", data)) {
                zones.add(Zone.create(name));
            }
            return zones;
        }
    }

    static enum RecordIdsDecoder implements Parser<List<String>> {
        INSTANCE;

        @Override
        public List<String> apply(JsonReader reader) throws IOException {
            JsonArray data;
            try {
                data = new JsonParser().parse(reader).getAsJsonArray();
            } catch (JsonIOException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            }
            return toFirstGroup("/REST/([a-zA-Z]+Record/[^\"]+/[^\"]+/[0-9]+)", data);
        }
    }

    static enum RecordsByNameAndTypeDecoder implements Parser<Iterator<Record>> {
        INSTANCE;

        @Override
        public Iterator<Record> apply(JsonReader reader) throws IOException {
            JsonArray data;
            try {
                data = new JsonParser().parse(reader).getAsJsonArray();
            } catch (JsonIOException e) {
                if (e.getCause() != null && e.getCause() instanceof IOException) {
                    throw IOException.class.cast(e.getCause());
                }
                throw e;
            }
            List<Record> records = new ArrayList<Record>();
            for (JsonElement datum : data) {
                records.add(ToRecord.INSTANCE.apply(datum));
            }
            return records.iterator();
        }
    }

    private final Parser<T> fn;

    /**
     * You must subclass this, in order to prevent type erasure on {@code T}. In
     * addition to making a concrete type, you can also use the following form.
     * <p/>
     * <br>
     * <p/>
     * 
     * <pre>
     * new DynECTDecoder&lt;Foo&gt;(sessionValid, fn) {
     * }; // note the curly braces ensures no type erasure!
     * </pre>
     */
    protected DynECTDecoder(Parser<T> fn) {
        this.fn = fn;
    }

    @Override
    public T decode(Reader ireader, Type ignored) throws IOException {
        JsonReader reader = new JsonReader(ireader);
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String nextName = reader.nextName();
                if ("data".equals(nextName)) {
                    return fn.apply(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            throw new IllegalStateException("no data returned in response");
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
