package denominator.route53;

import static java.lang.String.format;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import denominator.model.ResourceRecordSet;

enum SerializeRRS implements Function<ResourceRecordSet<?>, String> {
    INSTANCE;
    @Override
    public String apply(ResourceRecordSet<?> rrs) {
        Map<String, Object> ext = Maps.newLinkedHashMap();
        for (Map<String, Object> profile : rrs.profiles())
            ext.putAll(profile);
        StringBuilder builder = new StringBuilder().append("<ResourceRecordSet>");
        builder.append("<Name>").append(rrs.name()).append("</Name>");
        builder.append("<Type>").append(rrs.type()).append("</Type>");
        if (rrs.qualifier().isPresent())
            builder.append("<SetIdentifier>")//
                    .append(rrs.qualifier().get())//
                    .append("</SetIdentifier>");
        // note lowercase as this is a supported profile
        if (ext.containsKey("weight"))
            builder.append("<Weight>").append(ext.get("weight")).append("</Weight>");
        if (ext.containsKey("Region"))
            builder.append("<Region>").append(ext.get("Region")).append("</Region>");
        if (ext.containsKey("HostedZoneId")) {
            builder.append("<AliasTarget>");
            builder.append("<HostedZoneId>").append(ext.get("HostedZoneId")).append("</HostedZoneId>");
            builder.append("<DNSName>").append(ext.get("DNSName")).append("</DNSName>");
            builder.append("</AliasTarget>");
        } else {
            // default ttl from the amazon console is 300
            builder.append("<TTL>").append(rrs.ttl().or(300)).append("</TTL>");
            builder.append("<ResourceRecords>");
            for (Map<String, Object> data : rrs.rdata()) {
                String textFormat = Joiner.on(' ').join(data.values());
                if (ImmutableSet.of("SPF", "TXT").contains(rrs.type())) {
                    textFormat = format("\"%s\"", textFormat);
                }
                builder.append("<ResourceRecord>").append("<Value>").append(textFormat).append("</Value>")
                        .append("</ResourceRecord>");
            }
            builder.append("</ResourceRecords>");
        }
        return builder.append("</ResourceRecordSet>").toString();
    }
}
