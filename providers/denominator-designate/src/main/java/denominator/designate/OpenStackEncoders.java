package denominator.designate;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import denominator.designate.OpenStackApis.Record;
import feign.RequestTemplate;
import feign.codec.BodyEncoder;

class OpenStackEncoders {
    static class RecordEncoder implements BodyEncoder {
        /**
         * As formats are stable, using normal json to create the string as
         * opposed to reflection.
         */
        @Override
        public void encodeBody(Object bodyParam, RequestTemplate base) {
            Record record = Record.class.cast(bodyParam);
            if (record.id != null)
                base.append("/").append(record.id);
            JsonObject asJson = new JsonObject();
            asJson.add("name", new JsonPrimitive(record.name));
            asJson.add("type", new JsonPrimitive(record.type));
            if (record.ttl != null)
                asJson.add("ttl", new JsonPrimitive(record.ttl));
            asJson.add("data", new JsonPrimitive(record.data));
            if (record.priority != null)
                asJson.add("priority", new JsonPrimitive(record.priority));
            base.body(asJson.toString());
        }
    }
}
