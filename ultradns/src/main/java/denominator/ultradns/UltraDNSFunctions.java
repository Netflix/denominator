package denominator.ultradns;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.model.rdata.TXTData;

import static denominator.common.Util.join;

final class UltraDNSFunctions {

  private UltraDNSFunctions() { /* */
  }

  static Map<String, Object> forTypeAndRData(String type, List<String> rdata) {
    if ("A".equals(type)) {
      return AData.create(rdata.get(0));
    } else if ("AAAA".equals(type)) {
      return AAAAData.create(rdata.get(0));
    } else if ("CNAME".equals(type)) {
      return CNAMEData.create(rdata.get(0));
    } else if ("MX".equals(type)) {
      return MXData.create(Integer.valueOf(rdata.get(0)), rdata.get(1));
    } else if ("NS".equals(type)) {
      return NSData.create(rdata.get(0));
    } else if ("PTR".equals(type)) {
      return PTRData.create(rdata.get(0));
    } else if ("SOA".equals(type)) {
      return SOAData.builder().mname(rdata.get(0)).rname(rdata.get(1))
          .serial(Integer.valueOf(rdata.get(2)))
          .refresh(Integer.valueOf(rdata.get(3))).retry(Integer.valueOf(rdata.get(4)))
          .expire(Integer.valueOf(rdata.get(5))).minimum(Integer.valueOf(rdata.get(6))).build();
    } else if ("SPF".equals(type)) {
      return SPFData.create(rdata.get(0));
    } else if ("SRV".equals(type)) {
      return SRVData.builder().priority(Integer.valueOf(rdata.get(0)))
          .weight(Integer.valueOf(rdata.get(1)))
          .port(Integer.valueOf(rdata.get(2))).target(rdata.get(3)).build();
    } else if ("SSHFP".equals(type)) {
      return SSHFPData.builder().algorithm(Integer.valueOf(rdata.get(0)))
          .fptype(Integer.valueOf(rdata.get(1)))
          .fingerprint(rdata.get(2)).build();
    } else if ("TXT".equals(type)) {
      return TXTData.create(rdata.get(0));
    } else {
      Map<String, Object> map = new LinkedHashMap<String, Object>();
      map.put("rdata", join(' ', rdata));
      return map;
    }
  }
}
