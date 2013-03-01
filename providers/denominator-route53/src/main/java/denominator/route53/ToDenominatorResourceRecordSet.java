package denominator.route53;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;

import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
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

/**
 * Amazon does group by record sets. However, the {@code rdata} is in text
 * format. This will lazily parse the record sets into the normalized format.
 * Note this cannot parse records who match {@link #isAlias()}
 */
enum ToDenominatorResourceRecordSet implements
        Function<org.jclouds.route53.domain.ResourceRecordSet, ResourceRecordSet<?>> {
    INSTANCE;

    @Override
    public ResourceRecordSet<?> apply(org.jclouds.route53.domain.ResourceRecordSet input) {
        Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
                                                                .name(input.getName())
                                                                .type(input.getType());
        if (input.getTTL().isPresent())
            builder.ttl(input.getTTL().get().intValue());
        for (String rdata : input.getValues()) {
            builder.add(parseTextFormat(input.getType(), rdata));
        }
        return builder.build();
    }


    /**
     * is this an alias to record values defined outside the API?
     * 
     * @see <a
     *      href="http://docs.aws.amazon.com/Route53/latest/DeveloperGuide/CreatingAliasRRSets.html">alias
     *      RRSet</a>
     */
    static final Predicate<org.jclouds.route53.domain.ResourceRecordSet> isAlias() {
        return new Predicate<org.jclouds.route53.domain.ResourceRecordSet>() {
            @Override
            public boolean apply(org.jclouds.route53.domain.ResourceRecordSet input) {
                return input.getAliasTarget().isPresent();
            }

            @Override
            public String toString() {
                return "IsAlias()";
            }
        };
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
            return MXData.create(UnsignedInteger.valueOf(parts.get(0)), parts.get(1));
        } else if ("NS".equals(type)) {
            return NSData.create(rdata);
        } else if ("PTR".equals(type)) {
            return PTRData.create(rdata);
        } else if ("SOA".equals(type)) {
            ImmutableList<String> parts = split(rdata);
            return SOAData.builder()
                          .mname(parts.get(0))
                          .rname(parts.get(1))
                          .serial(UnsignedInteger.valueOf(parts.get(2)))
                          .refresh(UnsignedInteger.valueOf(parts.get(3)))
                          .retry(UnsignedInteger.valueOf(parts.get(4)))
                          .expire(UnsignedInteger.valueOf(parts.get(5)))
                          .minimum(UnsignedInteger.valueOf(parts.get(6))).build();
        } else if ("SPF".equals(type)) {
            return SPFData.create(rdata);
        } else if ("SRV".equals(type)) {
            ImmutableList<String> parts = split(rdata);
            return SRVData.builder()
                          .priority(UnsignedInteger.valueOf(parts.get(0)))
                          .weight(UnsignedInteger.valueOf(parts.get(1)))
                          .port(UnsignedInteger.valueOf(parts.get(2)))
                          .target(parts.get(3)).build();
        } else if ("TXT".equals(type)) {
            return TXTData.create(rdata);
        } else {
            return ImmutableMap.<String, Object> of("rdata", rdata);
        }
    }

    private static ImmutableList<String> split(String rdata) {
        return ImmutableList.copyOf(Splitter.on(' ').split(rdata));
    }
}