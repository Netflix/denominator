package denominator.ultradns;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Joiner;

import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.Record;
import feign.RequestTemplate;
import feign.codec.FormEncoder;

class UltraDNSFormEncoders {
    static class ZoneAndResourceRecord implements FormEncoder {

        @Override
        public void encodeForm(Map<String, ?> formParams, RequestTemplate base) {
            String zoneName = formParams.get("zoneName").toString();
            Record record = Record.class.cast(formParams.get("resourceRecord"));
            String xml = toXML(zoneName, record);
            if (record.id != null) {
                xml = update(record.id, xml);
            }
            base.body(xml);
        }

        static String toXML(String zoneName, Record record) {
            StringBuilder values = new StringBuilder("<InfoValues");
            for (int i = 0; i < record.rdata.size(); i++) {
                values.append(' ').append("Info").append(i + 1).append("Value=").append('"')
                        .append(record.rdata.get(i)).append('"');
            }
            values.append(" />");
            return format(CREATE_RR_TEMPLATE, zoneName, record.typeCode, record.name, record.ttl, values.toString());
        }

        static String update(Object guid, String xml) {
            return xml.replace("createResourceRecord", "updateResourceRecord").replace("<resourceRecord",
                    format("<resourceRecord Guid=\"%s\"", guid));
        }
    }

    static class RecordAndDirectionalGroup implements FormEncoder {

        @Override
        public void encodeForm(Map<String, ?> formParams, RequestTemplate base) {
            DirectionalRecord record = DirectionalRecord.class.cast(formParams.get("record"));
            DirectionalGroup group = DirectionalGroup.class.cast(formParams.get("group"));
            String xml = toXML(formParams.get("poolId"), record, group);
            base.body(xml);
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
            recordConfig.append("<DirectionalRecordConfiguration recordType=\"").append(record.type).append('"');
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
                    record.ttl, values(record));
        }

        private static String values(DirectionalRecord record) {
            StringBuilder values = new StringBuilder("<InfoValues");
            for (int i = 0; i < record.rdata.size(); i++) {
                values.append(' ').append("Info").append(i + 1).append("Value=").append('"')
                        .append(record.rdata.get(i)).append('"');
            }
            values.append(" />");
            return values.toString();
        }

        private static String geo(DirectionalGroup group) {
            StringBuilder groupData = new StringBuilder();
            groupData.append("<GeolocationGroupDetails groupName=\"").append(group.name).append("\">");
            for (Entry<String, Collection<String>> region : group.regionToTerritories.asMap().entrySet()) {
                groupData.append("<GeolocationGroupDefinitionData regionName=\"").append(region.getKey()).append('"');
                groupData.append(" territoryNames=\"").append(Joiner.on(';').join(region.getValue())).append("\" />");
            }
            groupData.append("</GeolocationGroupDetails>");
            return groupData.toString();
        }
    }

    private static final String CREATE_RR_TEMPLATE = "<v01:createResourceRecord><transactionID /><resourceRecord ZoneName=\"%s\" Type=\"%s\" DName=\"%s\" TTL=\"%s\">%s</resourceRecord></v01:createResourceRecord>";
    private static final String CREATE_DR_TEMPLATE = "<v01:addDirectionalPoolRecord><transactionID />%s%s%s<forceOverlapTransfer>true</forceOverlapTransfer></AddDirectionalRecordData></v01:addDirectionalPoolRecord>";
    private static final String UPDATE_DR_TEMPLATE = "<v01:updateDirectionalPoolRecord><transactionID /><UpdateDirectionalRecordData directionalPoolRecordId=\"%s\">%s%s<forceOverlapTransfer>true</forceOverlapTransfer></UpdateDirectionalRecordData></v01:updateDirectionalPoolRecord>";
    private static final String CREATE_DGROUP_TEMPLATE = "<GeolocationGroupData><GroupData groupingType=\"DEFINE_NEW_GROUP\" />%s</GeolocationGroupData>";
}
