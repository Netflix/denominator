package denominator.route53;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;

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

class ResourceRecordSetHandler extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {
    private List<String> profiles = ImmutableList.of("Failover", "Region", "HealthCheckId");

    private StringBuilder currentText = new StringBuilder();
    private Builder<Map<String, Object>> builder = ResourceRecordSet.builder();
    private ImmutableList.Builder<Map<String, Object>> profile = ImmutableList.builder();
    private ImmutableMap.Builder<String, Object> alias = ImmutableMap.<String, Object> builder();

    @Override
    public ResourceRecordSet<?> getResult() {
        try {
            if (!alias.build().isEmpty()) {
                profile.add(alias.put("type", "alias").build());
            }
            return builder.profile(profile.build()).build();
        } finally {
            builder = ResourceRecordSet.builder();
            profile = ImmutableList.builder();
            alias = ImmutableMap.<String, Object> builder();
            currentType =  null;
        }
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
            builder.ttl(Ints.tryParse(currentText.toString().trim()));
        } else if (qName.equals("Value")) {
            builder.add(parseTextFormat(currentType, currentText.toString().trim()));
        } else if (ImmutableSet.of("HostedZoneId", "DNSName", "EvaluateTargetHealth").contains(qName)) {
            alias.put(qName, currentText.toString().trim());
        } else if (qName.equals("SetIdentifier")) {
            builder.qualifier(currentText.toString().trim());
        } else if ("Weight".equals(qName)) {
            profile.add(Weighted.create(Ints.tryParse(currentText.toString().trim())));
        } else if (profiles.contains(qName)) {
            addProfile(qName, currentText.toString().trim());
        }
        currentText = new StringBuilder();
    }

    @Override
    public void characters(char ch[], int start, int length) {
        currentText.append(ch, start, length);
    }

    private void addProfile(String key, Object value) {
        profile.add(ImmutableMap.<String, Object> builder() //
                .put("type", UPPER_CAMEL.to(LOWER_CAMEL, key)) //
                .put(key, value).build());
    }

    /**
     * @see <a
     *      href="http://aws.amazon.com/route53/faqs/#Supported_DNS_record_types">supported
     *      types</a>
     * @see <a
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
            ImmutableList<String> parts = split(rdata);
            return MXData.create(Integer.valueOf(parts.get(0)), parts.get(1));
        } else if ("NS".equals(type)) {
            return NSData.create(rdata);
        } else if ("PTR".equals(type)) {
            return PTRData.create(rdata);
        } else if ("SOA".equals(type)) {
            ImmutableList<String> parts = split(rdata);
            return SOAData.builder().mname(parts.get(0)).rname(parts.get(1)).serial(Integer.valueOf(parts.get(2)))
                    .refresh(Integer.valueOf(parts.get(3))).retry(Integer.valueOf(parts.get(4)))
                    .expire(Integer.valueOf(parts.get(5))).minimum(Integer.valueOf(parts.get(6))).build();
        } else if ("SPF".equals(type)) {
            // unquote
            return SPFData.create(rdata.substring(1, rdata.length() - 1));
        } else if ("SRV".equals(type)) {
            ImmutableList<String> parts = split(rdata);
            return SRVData.builder().priority(Integer.valueOf(parts.get(0))).weight(Integer.valueOf(parts.get(1)))
                    .port(Integer.valueOf(parts.get(2))).target(parts.get(3)).build();
        } else if ("TXT".equals(type)) {
            // unquote
            return TXTData.create(rdata.substring(1, rdata.length() - 1));
        } else {
            return ImmutableMap.<String, Object> of("rdata", rdata);
        }
    }

    private static ImmutableList<String> split(String rdata) {
        return ImmutableList.copyOf(Splitter.on(' ').split(rdata));
    }
}
