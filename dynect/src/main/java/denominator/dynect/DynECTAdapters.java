package denominator.dynect;

import static denominator.common.Preconditions.checkState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import denominator.dynect.DynECT.Data;
import denominator.dynect.DynECT.Record;
import denominator.model.Zone;

/**
 * DynECT json includes an envelope called "data". These type adapters apply at
 * that level.
 * 
 * @see DynECTDecoder
 */
class DynECTAdapters {

    private DynECTAdapters() {
        // no instances.
    }

    static class NothingForbiddenAdapter extends DataAdapter<Boolean> {

        @Override
        public Boolean build(JsonReader reader) throws IOException {
            return new JsonParser().parse(reader).getAsJsonObject().get("forbidden").getAsJsonArray().size() == 0;
        }
    }

    static class TokenAdapter extends DataAdapter<String> {

        @Override
        public String build(JsonReader reader) throws IOException {
            return new JsonParser().parse(reader).getAsJsonObject().get("token").getAsString();
        }
    }

    static class ZonesAdapter extends DataAdapter<List<Zone>> {

        @Override
        public List<Zone> build(JsonReader reader) throws IOException {
            JsonArray data = new JsonParser().parse(reader).getAsJsonArray();
            List<Zone> zones = new ArrayList<Zone>();
            for (String name : toFirstGroup("/REST.*/([^/]+)/?$", data)) {
                zones.add(Zone.create(name));
            }
            return zones;
        }
    }

    static class RecordIdsAdapter extends DataAdapter<List<String>> {

        @Override
        public List<String> build(JsonReader reader) throws IOException {
            JsonArray data = new JsonParser().parse(reader).getAsJsonArray();
            return toFirstGroup("/REST/([a-zA-Z]+Record/[^\"]+/[^\"]+/[0-9]+)", data);
        }
    }

    static class RecordsByNameAndTypeAdapter extends DataAdapter<Iterator<Record>> {

        @Override
        public Iterator<Record> build(JsonReader reader) throws IOException {
            JsonArray data;
            data = new JsonParser().parse(reader).getAsJsonArray();
            List<Record> records = new ArrayList<Record>();
            for (JsonElement datum : data) {
                records.add(ToRecord.INSTANCE.apply(datum));
            }
            return records.iterator();
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

    static abstract class DataAdapter<X> extends TypeAdapter<Data<X>> {

        protected abstract X build(JsonReader reader) throws IOException;

        @Override
        public Data<X> read(JsonReader reader) throws IOException {
            Data<X> data = new Data<X>();
            reader.beginObject();
            while (reader.hasNext()) {
                String nextName = reader.nextName();
                if ("data".equals(nextName)) {
                    try {
                        data.data = build(reader);
                    } catch (JsonIOException e) {
                        if (e.getCause() != null && e.getCause() instanceof IOException) {
                            throw IOException.class.cast(e.getCause());
                        }
                        throw e;
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return data;
        }

        @Override
        public void write(JsonWriter out, Data<X> value) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
