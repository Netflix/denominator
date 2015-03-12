package denominator.ultradns;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.Record;
import feign.RequestTemplate;
import feign.codec.Encoder;

import static denominator.common.Util.join;
import static java.lang.String.format;

class UltraDNSFormEncoder implements Encoder {

  private static final String
      CREATE_RR_TEMPLATE =
      "<v01:createResourceRecord><transactionID /><resourceRecord ZoneName=\"%s\" Type=\"%s\" DName=\"%s\" TTL=\"%s\">%s</resourceRecord></v01:createResourceRecord>";
  private static final String
      CREATE_DR_TEMPLATE =
      "<v01:addDirectionalPoolRecord><transactionID />%s%s%s<forceOverlapTransfer>true</forceOverlapTransfer></AddDirectionalRecordData></v01:addDirectionalPoolRecord>";
  private static final String
      UPDATE_DR_TEMPLATE =
      "<v01:updateDirectionalPoolRecord><transactionID /><UpdateDirectionalRecordData directionalPoolRecordId=\"%s\">%s%s<forceOverlapTransfer>true</forceOverlapTransfer></UpdateDirectionalRecordData></v01:updateDirectionalPoolRecord>";
  private static final String
      CREATE_DGROUP_TEMPLATE =
      "<GeolocationGroupData><GroupData groupingType=\"DEFINE_NEW_GROUP\" />%s</GeolocationGroupData>";

  static String encodeZoneAndResourceRecord(Map<String, ?> formParams) {
    String zoneName = formParams.get("zoneName").toString();
    Record record = Record.class.cast(formParams.get("resourceRecord"));
    String xml = toXML(zoneName, record);
    if (record.id != null) {
      xml = update(record.id, xml);
    }
    return xml;
  }

  static String toXML(String zoneName, Record record) {
    StringBuilder values = new StringBuilder("<InfoValues");
    for (int i = 0; i < record.rdata.size(); i++) {
      values.append(' ').append("Info").append(i + 1).append("Value=").append('"')
          .append(record.rdata.get(i))
          .append('"');
    }
    values.append(" />");
    return format(CREATE_RR_TEMPLATE, zoneName, record.typeCode, record.name, record.ttl,
                  values.toString());
  }

  static String update(Object guid, String xml) {
    return xml.replace("createResourceRecord", "updateResourceRecord").replace("<resourceRecord",
                                                                               format(
                                                                                   "<resourceRecord Guid=\"%s\"",
                                                                                   guid));
  }

  static String encodeRecordAndDirectionalGroup(Map<String, ?> formParams) {
    DirectionalRecord record = DirectionalRecord.class.cast(formParams.get("record"));
    DirectionalGroup group = DirectionalGroup.class.cast(formParams.get("group"));
    return toXML(formParams.get("poolId"), record, group);
  }

  static String toXML(Object poolId, DirectionalRecord record, DirectionalGroup group) {
    if (poolId == null) {
      return format(UPDATE_DR_TEMPLATE, record.id, updateRecord(record), geo(group));
    }
    String addRecordToPool = format("<AddDirectionalRecordData directionalPoolId=\"%s\">", poolId);
    return format(CREATE_DR_TEMPLATE, addRecordToPool, createRecord(record),
                  format(CREATE_DGROUP_TEMPLATE, geo(group)));
  }

  private static String createRecord(DirectionalRecord record) {
    StringBuilder recordConfig = new StringBuilder();
    recordConfig.append("<DirectionalRecordConfiguration recordType=\"").append(record.type)
        .append('"');
    recordConfig.append(" TTL=\"").append(record.ttl).append("\" >");
    recordConfig.append(values(record));
    recordConfig.append("</DirectionalRecordConfiguration>");
    return recordConfig.toString();
  }

  /**
   * don't pass type or is no response when updating
   */
  private static String updateRecord(DirectionalRecord record) {
    return format("<DirectionalRecordConfiguration TTL=\"%s\" >%s</DirectionalRecordConfiguration>",
                  record.ttl,
                  values(record));
  }

  private static String values(DirectionalRecord record) {
    StringBuilder values = new StringBuilder("<InfoValues");
    for (int i = 0; i < record.rdata.size(); i++) {
      values.append(' ').append("Info").append(i + 1).append("Value=").append('"')
          .append(record.rdata.get(i))
          .append('"');
    }
    values.append(" />");
    return values.toString();
  }

  private static String geo(DirectionalGroup group) {
    StringBuilder groupData = new StringBuilder();
    groupData.append("<GeolocationGroupDetails groupName=\"").append(group.name).append("\">");
    for (Entry<String, Collection<String>> region : group.regionToTerritories.entrySet()) {
      groupData.append("<GeolocationGroupDefinitionData regionName=\"").append(region.getKey())
          .append('"');
      groupData.append(" territoryNames=\"").append(join(';', region.getValue().toArray()))
          .append("\" />");
    }
    groupData.append("</GeolocationGroupDetails>");
    return groupData.toString();
  }

  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template) {
    Map<String, ?> formParams = (Map<String, ?>) object;
    if (formParams.containsKey("zoneName")) {
      template.body(encodeZoneAndResourceRecord(formParams));
    } else {
      template.body(encodeRecordAndDirectionalGroup(formParams));
    }
  }
}
