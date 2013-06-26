package denominator.dynect;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import denominator.dynect.DynECT.Record;

enum ToRecord implements Function<JsonElement, Record> {
    INSTANCE;
    @Override
    public Record apply(JsonElement element) {
        JsonObject current = element.getAsJsonObject();
        Record record = new Record();
        record.id = current.get("record_id").getAsLong();
        record.name = current.get("fqdn").getAsString();
        record.type = current.get("record_type").getAsString();
        record.ttl = current.get("ttl").getAsInt();
        record.rdata = toRData(current.get("rdata").getAsJsonObject());
        return record;
    }

    static Map<String, Object> toRData(JsonObject rdata) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object> builder();
        for (Entry<String, JsonElement> entry : rdata.entrySet()) {
            // values are never nested
            JsonPrimitive value = entry.getValue().getAsJsonPrimitive();
            if (value.isNumber()) {
                builder.put(entry.getKey(), value.getAsInt());
            } else {
                builder.put(entry.getKey(), value.getAsString());
            }
        }
        return builder.build();
    }
}
