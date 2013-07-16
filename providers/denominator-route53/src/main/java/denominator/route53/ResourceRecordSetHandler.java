package denominator.route53;

import static denominator.common.Util.split;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

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

class ResourceRecordSetHandler extends DefaultHandler implements
        feign.codec.SAXDecoder.ContentHandlerWithResult<ResourceRecordSet<?>> {

    @Inject
    ResourceRecordSetHandler() {
    }

    private static final List<String> profileFields = Arrays.asList("Failover", "Region", "HealthCheckId");
    private static final List<String> aliasFields = Arrays.asList("HostedZoneId", "DNSName", "EvaluateTargetHealth");

    private StringBuilder currentText = new StringBuilder();
    private Builder<Map<String, Object>> builder = ResourceRecordSet.builder();
    private List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>();
    private Map<String, Object> alias = new LinkedHashMap<String, Object>();

    @Override
    public ResourceRecordSet<?> result() {
        if (!alias.isEmpty()) {
            alias.put("type", "alias");
            profiles.add(alias);
        }
        return builder.profile(profiles).build();
    }

    @Override
    public void startElement(String url, String name, String qName, Attributes attributes) {
    }

    private String currentType = null;

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
        } else if (aliasFields.contains(qName)) {
            alias.put(qName, currentText.toString().trim());
        } else if (qName.equals("SetIdentifier")) {
            builder.qualifier(currentText.toString().trim());
        } else if ("Weight".equals(qName)) {
            profiles.add(Weighted.create(Integer.parseInt(currentText.toString().trim())));
        } else if (profileFields.contains(qName)) {
            addProfile(qName, currentText.toString().trim());
        }
        currentText = new StringBuilder();
    }

    @Override
    public void characters(char ch[], int start, int length) {
        currentText.append(ch, start, length);
    }

    private void addProfile(String key, Object value) {
        Map<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("type", lowerFirst(key));
        profile.put(key, value);
        profiles.add(profile);
    }

    private static String lowerFirst(String key) {
        char c[] = key.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

    /**
     * See <a
     *      href="http://aws.amazon.com/route53/faqs/#Supported_DNS_record_types">supported
     *      types</a>
     * See <a
     *      href="http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/ResourceRecordTypes.html">record
     *      type formats</a>
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
            return SOAData.builder().mname(parts.get(0)).rname(parts.get(1)).serial(Integer.valueOf(parts.get(2)))
                    .refresh(Integer.valueOf(parts.get(3))).retry(Integer.valueOf(parts.get(4)))
                    .expire(Integer.valueOf(parts.get(5))).minimum(Integer.valueOf(parts.get(6))).build();
        } else if ("SPF".equals(type)) {
            // unquote
            return SPFData.create(rdata.substring(1, rdata.length() - 1));
        } else if ("SRV".equals(type)) {
            List<String> parts = split(' ', rdata);
            return SRVData.builder().priority(Integer.valueOf(parts.get(0))).weight(Integer.valueOf(parts.get(1)))
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
}
