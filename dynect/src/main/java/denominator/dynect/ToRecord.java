package denominator.dynect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import denominator.dynect.DynECT.Record;

enum ToRecord {
  INSTANCE;

  static Map<String, Object> toRData(JsonObject rdata) {
    Map<String, Object> builder = new LinkedHashMap<String, Object>();
    for (Entry<String, JsonElement> entry : rdata.entrySet()) {
      // values are never nested
      JsonPrimitive value = entry.getValue().getAsJsonPrimitive();
      if (value.isNumber()) {
        builder.put(entry.getKey(), value.getAsInt());
      } else {
        builder.put(entry.getKey(), value.getAsString());
      }
    }
    return builder;
  }

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
}
