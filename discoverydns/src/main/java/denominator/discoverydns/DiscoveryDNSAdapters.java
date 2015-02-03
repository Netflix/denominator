package denominator.discoverydns;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.common.Util;
import denominator.discoverydns.DiscoveryDNS.ResourceRecords;
import denominator.model.ResourceRecordSet;

final class DiscoveryDNSAdapters {

  static final class ResourceRecordsAdapter extends TypeAdapter<ResourceRecords> {

    @Override
    public void write(JsonWriter jsonWriter, ResourceRecords records)
        throws IOException {
      jsonWriter.beginArray();
      for (ResourceRecordSet<?> rrset : records.records) {
        for (Map<String, Object> rdata : rrset.records()) {
          jsonWriter.beginObject();
          jsonWriter.name("name").value(rrset.name());
          jsonWriter.name("class").value("IN");
          jsonWriter.name("ttl").value(rrset.ttl() == null ? "3600"
                                                           : rrset.ttl().toString());
          jsonWriter.name("type").value(rrset.type());
          jsonWriter.name("rdata").value(Util.flatten(rdata));
          jsonWriter.endObject();
        }
      }
      jsonWriter.endArray();
    }

    @Override
    public ResourceRecords read(JsonReader in) throws IOException {
      Map<Query, Collection<String>>
          rrsets =
          new LinkedHashMap<Query, Collection<String>>();
      in.beginArray();
      while (in.hasNext()) {
        in.beginObject();
        Query query = new Query();
        String rdata = null;
        while (in.hasNext()) {
          String name = in.nextName();
          if (name.equals("name")) {
            query.name = in.nextString();
          } else if (name.equals("type")) {
            query.type = in.nextString();
          } else if (name.equals("ttl")) {
            query.ttl = in.nextInt();
          } else if (name.equals("rdata")) {
            rdata = in.nextString();
          } else {
            in.skipValue();
          }
        }
        in.endObject();
        if (!rrsets.containsKey(query)) {
          rrsets.put(query, new ArrayList<String>());
        }
        rrsets.get(query).add(rdata);
      }
      in.endArray();

      DiscoveryDNS.ResourceRecords ddnsRecords = new DiscoveryDNS.ResourceRecords();
      for (Map.Entry<Query, Collection<String>> entry : rrsets.entrySet()) {
        Query id = entry.getKey();
        ResourceRecordSet.Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
            .name(id.name)
            .type(id.type)
            .ttl(id.ttl);
        for (String rdata : entry.getValue()) {
          builder.add(Util.toMap(id.type, rdata));
        }
        ddnsRecords.records.add(builder.build());
      }
      return ddnsRecords;
    }
  }

  private static class Query {

    String name;
    String type;
    Integer ttl;

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((ttl == null) ? 0 : ttl.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      return hashCode() == obj.hashCode();
    }
  }
}
