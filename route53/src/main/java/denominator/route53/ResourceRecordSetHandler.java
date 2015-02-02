package denominator.route53;

import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

import static denominator.common.Util.split;

class ResourceRecordSetHandler extends DefaultHandler
    implements ContentHandlerWithResult<ResourceRecordSet<?>> {

  private final StringBuilder currentText = new StringBuilder();
  private final Builder<Map<String, Object>> builder = ResourceRecordSet.builder();
  private String currentType;
  private String hostedZoneId;
  private String dnsName;

  /**
   * See <a href="http://aws.amazon.com/route53/faqs/#Supported_DNS_record_types" >supported
   * types</a> See <a href= "http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/ResourceRecordTypes.html"
   * >record type formats</a>
   */
  static Map<String, Object> parseTextFormat(String type, String rdata) {
    if ("A".equals(type)) {
      return AData.create(rdata);
    } else if ("AAAA".equals(type)) {
      return AAAAData.create(rdata);
    } else if ("CNAME".equals(type)) {
      return CNAMEData.create(rdata);
    } else if ("MX".equals(type)) {
      List<String> parts = split(' ', rdata);
      return MXData.create(Integer.valueOf(parts.get(0)), parts.get(1));
    } else if ("NS".equals(type)) {
      return NSData.create(rdata);
    } else if ("PTR".equals(type)) {
      return PTRData.create(rdata);
    } else if ("SOA".equals(type)) {
      List<String> parts = split(' ', rdata);
      return SOAData.builder().mname(parts.get(0)).rname(parts.get(1))
          .serial(Integer.valueOf(parts.get(2)))
          .refresh(Integer.valueOf(parts.get(3))).retry(Integer.valueOf(parts.get(4)))
          .expire(Integer.valueOf(parts.get(5))).minimum(Integer.valueOf(parts.get(6))).build();
    } else if ("SPF".equals(type)) {
      // unquote
      return SPFData.create(rdata.substring(1, rdata.length() - 1));
    } else if ("SRV".equals(type)) {
      List<String> parts = split(' ', rdata);
      return SRVData.builder().priority(Integer.valueOf(parts.get(0)))
          .weight(Integer.valueOf(parts.get(1)))
          .port(Integer.valueOf(parts.get(2))).target(parts.get(3)).build();
    } else if ("TXT".equals(type)) {
      // unquote
      return TXTData.create(rdata.substring(1, rdata.length() - 1));
    } else {
      Map<String, Object> unknown = new LinkedHashMap<String, Object>();
      unknown.put("rdata", rdata);
      return unknown;
    }
  }

  @Override
  public ResourceRecordSet<?> result() {
    return builder.build();
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (qName.equals("Name")) {
      builder.name(currentText.toString().trim());
    } else if (qName.equals("Type")) {
      builder.type(currentType = currentText.toString().trim());
    } else if (qName.equals("TTL")) {
      builder.ttl(Integer.parseInt(currentText.toString().trim()));
    } else if (qName.equals("Value")) {
      builder.add(parseTextFormat(currentType, currentText.toString().trim()));
    } else if (qName.equals("SetIdentifier")) {
      builder.qualifier(currentText.toString().trim());
    } else if (qName.equals("HostedZoneId")) {
      hostedZoneId = currentText.toString().trim();
    } else if (qName.equals("DNSName")) {
      dnsName = currentText.toString().trim();
    } else if (qName.equals("AliasTarget")) {
      builder.add(AliasTarget.create(hostedZoneId, dnsName));
    } else if ("Weight".equals(qName)) {
      builder.weighted(Weighted.create(Integer.parseInt(currentText.toString().trim())));
    }
    currentText.setLength(0);
  }

  @Override
  public void characters(char ch[], int start, int length) {
    currentText.append(ch, start, length);
  }
}
