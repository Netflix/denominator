package denominator.route53;

import static denominator.common.Util.join;
import static java.lang.String.format;

import java.util.Map;

import denominator.model.ResourceRecordSet;

enum SerializeRRS {
    INSTANCE;

    public String apply(ResourceRecordSet<?> rrs) {
        StringBuilder builder = new StringBuilder().append("<ResourceRecordSet>");
        builder.append("<Name>").append(rrs.name()).append("</Name>");
        builder.append("<Type>").append(rrs.type()).append("</Type>");
        if (rrs.qualifier() != null)
            builder.append("<SetIdentifier>")//
                    .append(rrs.qualifier())//
                    .append("</SetIdentifier>");
        // note lowercase as this is a supported profile
        if (rrs.weighted() != null) {
            builder.append("<Weight>").append(rrs.weighted().weight()).append("</Weight>");
        }
        // default ttl from the amazon console is 300
        builder.append("<TTL>").append(rrs.ttl() == null ? 300 : rrs.ttl()).append("</TTL>");
        builder.append("<ResourceRecords>");
        for (Map<String, Object> data : rrs.records()) {
            String textFormat = join(' ', data.values().toArray());
            if ("SPF".equals(rrs.type()) || "TXT".equals(rrs.type())) {
                textFormat = format("\"%s\"", textFormat);
            }
            builder.append("<ResourceRecord>").append("<Value>").append(textFormat).append("</Value>")
                    .append("</ResourceRecord>");
        }
        builder.append("</ResourceRecords>");
        return builder.append("</ResourceRecordSet>").toString();
    }
}
