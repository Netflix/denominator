package denominator.dynect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import denominator.dynect.DynECT.Record;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;

enum ToRecord {
  INSTANCE;

  static Map<String, Object> toRData(String type, JsonObject rdata) {
    if ("A".equals(type)) {
      return AData.create(rdata.get("address").getAsString());
    } else if ("AAAA".equals(type)) {
      return AAAAData.create(rdata.get("address").getAsString());
    } else if ("CNAME".equals(type)) {
      return CNAMEData.create(rdata.get("cname").getAsString());
    } else if ("MX".equals(type)) {
      int preference = rdata.get("preference").getAsInt();
      String exchange = rdata.get("exchange").getAsString();
      return MXData.create(preference, exchange);
    } else if ("NS".equals(type)) {
      return NSData.create(rdata.get("nsdname").getAsString());
    } else if ("PTR".equals(type)) {
      return PTRData.create(rdata.get("ptrdname").getAsString());
    } else if ("SOA".equals(type)) {
      return SOAData.builder()
                 .rname(rdata.get("rname").getAsString())
                 .retry(rdata.get("retry").getAsInt())
                 .mname(rdata.get("mname").getAsString())
                 .minimum(rdata.get("minimum").getAsInt())
                 .refresh(rdata.get("refresh").getAsInt())
                 .expire(rdata.get("expire").getAsInt())
                 .serial(rdata.get("serial").getAsInt())
                 .build();
    } else if ("SPF".equals(type)) {
      return SPFData.create(rdata.get("txtdata").getAsString());
    } else if ("SRV".equals(type)) {
      return SRVData.builder()
                 .priority(rdata.get("priority").getAsInt())
                 .weight(rdata.get("weight").getAsInt())
                 .port(rdata.get("port").getAsInt())
                 .target(rdata.get("target").getAsString())
                 .build();
    } else if ("TXT".equals(type)) {
      return TXTData.create(rdata.get("txtdata").getAsString());
    } else if ("CERT".equals(type)) {
        return CERTData.builder()
                .format(rdata.get("format").getAsInt())
                .tag(rdata.get("tag").getAsInt())
                .algorithm(rdata.get("algorithm").getAsInt())
                .certificate(rdata.get("certificate").getAsString()).build();
    } else {
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
  }

  public Record apply(JsonElement element) {
    JsonObject current = element.getAsJsonObject();
    Record record = new Record();
    record.id = current.get("record_id").getAsLong();
    record.serviceClass =
        current.has("service_class") ? current.get("service_class").getAsString() : null;
    record.name = current.get("fqdn").getAsString();
    record.type = current.get("record_type").getAsString();
    record.ttl = current.get("ttl").getAsInt();
    record.rdata = toRData(record.type, current.get("rdata").getAsJsonObject());
    return record;
  }
}
