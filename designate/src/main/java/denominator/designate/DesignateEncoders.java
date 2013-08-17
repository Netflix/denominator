package denominator.designate;

import javax.inject.Inject;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import denominator.designate.Designate.Record;
import feign.codec.Encoder;

class DesignateEncoders {
    static class RecordEncoder implements Encoder.Text<Record> {
        @Inject
        RecordEncoder() {
        }

        /**
         * As formats are stable, using normal json to create the string as
         * opposed to reflection.
         */
        @Override
        public String encode(Record record) {
            JsonObject asJson = new JsonObject();
            asJson.add("name", new JsonPrimitive(record.name));
            asJson.add("type", new JsonPrimitive(record.type));
            if (record.ttl != null)
                asJson.add("ttl", new JsonPrimitive(record.ttl));
            asJson.add("data", new JsonPrimitive(record.data));
            if (record.priority != null)
                asJson.add("priority", new JsonPrimitive(record.priority));
            return asJson.toString();
        }
    }
}