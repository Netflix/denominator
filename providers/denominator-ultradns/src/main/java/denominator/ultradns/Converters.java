
package denominator.ultradns;

import java.util.List;
import java.util.Map;

import org.jclouds.ultradns.ws.domain.ResourceRecord;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;

import denominator.ResourceTypeToValue;
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

final class Converters {
    private Converters() { /* */}

    
    static Map<String, Object> toRdataMap(ResourceRecord record) {
        List<String> parts = record.getRData();
        String type = new ResourceTypeToValue().inverse().get(record.getType());
        if ("A".equals(type)) {
            return AData.create(parts.get(0));
        } else if ("AAAA".equals(type)) {
            return AAAAData.create(parts.get(0));
        } else if ("CNAME".equals(type)) {
            return CNAMEData.create(parts.get(0));
        } else if ("MX".equals(type)) {
            return MXData.create(UnsignedInteger.valueOf(parts.get(0)), parts.get(1));
        } else if ("NS".equals(type)) {
            return NSData.create(parts.get(0));
        } else if ("PTR".equals(type)) {
            return PTRData.create(parts.get(0));
        } else if ("SOA".equals(type)) {
            return SOAData.builder()
                          .mname(parts.get(0))
                          .rname(parts.get(1))
                          .serial(UnsignedInteger.valueOf(parts.get(2)))
                          .refresh(UnsignedInteger.valueOf(parts.get(3)))
                          .retry(UnsignedInteger.valueOf(parts.get(4)))
                          .expire(UnsignedInteger.valueOf(parts.get(5)))
                          .minimum(UnsignedInteger.valueOf(parts.get(6))).build();
        } else if ("SPF".equals(type)) {
            return SPFData.create(parts.get(0));
        } else if ("SRV".equals(type)) {
            return SRVData.builder()
                          .priority(UnsignedInteger.valueOf(parts.get(0)))
                          .weight(UnsignedInteger.valueOf(parts.get(1)))
                          .port(UnsignedInteger.valueOf(parts.get(2)))
                          .target(parts.get(3)).build();
        } else if ("TXT".equals(type)) {
            return TXTData.create(parts.get(0));
        } else {
            return ImmutableMap.<String, Object> of("rdata", Joiner.on(' ').join(parts));
        }
    }
}
