package denominator.route53;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.route53.Route53.ActionOnResourceRecordSet;
import feign.RequestTemplate;
import feign.codec.Encoder;

import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.join;
import static java.lang.String.format;

class EncodeChanges implements Encoder {

  static String apply(ResourceRecordSet<?> rrs) {
    StringBuilder builder = new StringBuilder().append("<ResourceRecordSet>");
    builder.append("<Name>").append(rrs.name()).append("</Name>");
    builder.append("<Type>").append(rrs.type()).append("</Type>");
    if (rrs.qualifier() != null) {
      builder.append("<SetIdentifier>")//
          .append(rrs.qualifier())//
          .append("</SetIdentifier>");
    }
    // note lowercase as this is a supported profile
    if (rrs.weighted() != null) {
      builder.append("<Weight>").append(rrs.weighted().weight()).append("</Weight>");
    }
    if (rrs.records().size() == 1 && rrs.records().get(0).containsKey("HostedZoneId")) {
      builder.append("<AliasTarget>");
      Map<String, Object> aliasTarget = rrs.records().get(0);
      for (String attribute : new String[]{"HostedZoneId", "DNSName"}) {
        builder.append('<').append(attribute).append('>');
        builder.append(
            checkNotNull(aliasTarget.get(attribute), "missing %s in alias target %s", attribute,
                         rrs));
        builder.append("</").append(attribute).append('>');
      }
      // not valid until health checks are supported.
      builder.append("<EvaluateTargetHealth>false</EvaluateTargetHealth>");
      builder.append("</AliasTarget>");
    } else {
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
    }
    return builder.append("</ResourceRecordSet>").toString();
  }

  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template) {
    List<ActionOnResourceRecordSet> actions = (List<ActionOnResourceRecordSet>) object;
    StringBuilder b = new StringBuilder();
    b.append(
        "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch>");
    b.append("<Changes>");
    for (ActionOnResourceRecordSet change : actions) {
      b.append("<Change>").append("<Action>").append(change.action).append("</Action>")
          .append(apply(change.rrs))
          .append("</Change>");
    }
    b.append("</Changes>");
    b.append("</ChangeBatch></ChangeResourceRecordSetsRequest>");
    template.body(b.toString());
  }
}
